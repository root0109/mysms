"""Checks the backend against the mechanical rules of JAVA-CODING-RULES.md, ARCHITECTURE.md and database/README.md
that a compiler, Checkstyle or SpotBugs do not already enforce, and prints every violation (exit code 1 if any):

    python3 tools/backend/conformance.py

  M0   backend/ holds only the Maven project: poms, sources, build configuration; no scripts or tooling
  R1   every SELECT / UPDATE / DELETE over a tenant table filters each tenant table it reads or changes with
       `tenant_id = :tenantId` at the statement's own level (not only inside a sub-select or an OR branch): the
       generator's SQL parser (crud/tenant_sql.py) must find nothing to add; UNION, comma joins, DELETE ... USING and
       UPDATE ... FROM are refused. Global tables and the tenant directory (read before a tenant is known) are exempt;
       the fragments listed in R1_FRAGMENTS are joined in Java and checked by their component tests
  R2   SQL lives in repositories, as constants (no SQL text in controllers, services or mappers)
  B1   no Transactional annotation of any package / TransactionTemplate; no ORM import or dependency (JPA, Hibernate,
       Spring Data, MyBatis, jOOQ); no ArchUnit or Modulith
  A1   a domain module depends only on core, contracts, sdk and auth-client (and messaging at runtime
       scope; no module code imports it)
  §3   packages are the layer roles of §3 only
  §5   every entity extends BaseEntity and names its TABLE
  N1   domain/ holds entities only (projections are records in repository/); entities carry exactly the Lombok set of
       §5.2, no Jackson or mapping annotations, no Map columns, no LocalDateTime; BaseEntity's audit set is exact;
       lifecycle hooks are called from business/ only
  R5   repositories are plain classes: no interfaces in repository/, no String.format/formatted SQL, no OFFSET paging;
       migrations define no database functions or procedures
  C3   every controller implements a generated contract interface; no @*Mapping, springdoc/Swagger or base controller;
       controllers import no repository, outbox, audit writer or event publisher
  M1   one @MapperConfig per module (unmappedTargetPolicy ERROR) used by every @Mapper; mappers are interfaces; no
       componentModel on a mapper, no null-value strategies, no @MappingTarget; a DTO -> entity method ignores id,
       tenantId and the whole audit set
  D2   no hand-written *DTO classes, no Optional fields in model/, no @JsonView / ResponseBodyAdvice; PUT only for the
       operations in PUT_ALLOWED; a delete operation answers 204
  E2   no @ControllerAdvice, own exception base type or BusinessException in a domain module; no exception message
       passed into a typed exception or the problem body
  X1   no Authorization header, servlet auth filter or token library (io.jsonwebtoken, com.nimbusds,
       org.springframework.security except its crypto package, which only iam uses for password hashing) outside
       auth-client, except iam's issuer (TOKEN_ISSUERS); the OIDC key set controller is the one controller of an
       infrastructure path (INFRASTRUCTURE_CONTROLLERS)
  §6   no hard DELETE (soft delete only), except the pruning of module-local infrastructure tables
       (idempotency_key, outbox_event, inbox_event)
  T    mocks and stub servers (Mockito, @MockBean/@MockitoBean/@SpyBean, WireMock, MockWebServer,
       MockRestServiceServer) only under src/test; no Spring context (@SpringBootTest) under src/test; every module has
       its ModuleBoundaryTest and service component test, and its component tests sign real tokens (TestTokens)
  §14  a module's migrations live in database/migrations/<module> and touch only its own schema; no cross-schema FK
"""
import glob
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE + '/crud')
import tenant_sql  # noqa: E402

BACKEND = os.path.normpath(HERE + '/../../backend')
REPO = os.path.normpath(BACKEND + '/..')
SHARED = ('core', 'contracts', 'sdk', 'auth-client', 'messaging', 'app')
ECLIPSE_MODULE_FILES = {'.classpath', '.factorypath', '.project', '.settings'}
PACKAGES = {'controller', 'grpc', 'business', 'repository', 'domain', 'model', 'mapper', 'validation', 'errors',
            'events', 'config', 'constants'}
EXEMPT_TABLES = {'iam.tenant_directory'}
# SQL fragments joined in Java (not complete statements, so the parser cannot place a predicate); each fragment states
# its own alias-qualified predicates and the assembled statements are covered by the class's component tests.
R1_FRAGMENTS = {'GrantRepository.java': 'permission sub-selects assembled from COUNTED_GRANT / NOT_DENIED'}
MOCKS = (r'org\.mockito|@MockBean\b|@MockitoBean\b|@SpyBean\b|@MockitoSpyBean\b|com\.github\.tomakehurst\.wiremock'
         r'|org\.wiremock|okhttp3\.mockwebserver|MockRestServiceServer')
# PUT is allowed only for an idempotent "set the value at this key" operation the contract defines (§7.3)
PUT_ALLOWED = {'iam.SetMySetting'}
# the token issuer signs tokens with the token library; nothing else outside auth-client may import one (§16.1)
TOKEN_ISSUERS = {'iam/src/main/java/com/mysms/iam/business/TokenIssuer.java'}
ENTITY_LOMBOK = ['@Getter', '@Setter', '@SuperBuilder', '@NoArgsConstructor',
                 '@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)']
AUDIT_SET = ['id', 'createdAt', 'createdBy', 'updatedAt', 'updatedBy', 'deletedAt', 'rowVersion']
TOKEN_LIBS = r'^import (io\.jsonwebtoken|com\.nimbusds|org\.springframework\.security\.(?!crypto\.))'
# iam hashes passwords (argon2id) with Spring Security's crypto package: password hashing, not token handling
PASSWORD_HASHING = r'^import org\.springframework\.security\.crypto\.'
# controllers of infrastructure paths that are not API operations (§16 step 1): the OIDC key set
INFRASTRUCTURE_CONTROLLERS = {'iam/src/main/java/com/mysms/iam/controller/JwksController.java'}
ORM_ARTIFACTS = r'<artifactId>(hibernate[\w-]*|spring-boot-starter-data-jpa|spring-data-[\w-]+|jakarta\.persistence-api|mybatis[\w-]*|jooq[\w-]*)</artifactId>'


def java(module, root='main'):
    return glob.glob('%s/%s/src/%s/java/**/*.java' % (BACKEND, module, root), recursive=True)


def main():
    modules = sorted(m for m in os.listdir(BACKEND) if os.path.exists('%s/%s/pom.xml' % (BACKEND, m)))
    domain = [m for m in modules if m not in SHARED]
    tenant_tables = tenant_sql.tenant_tables() - EXEMPT_TABLES
    problems = []

    def bad(rule, where, what):
        problems.append('%-4s %s: %s' % (rule, os.path.relpath(where, REPO), what))

    # M0: backend/ is the Maven multi-module project and nothing else (tooling lives in tools/ at the repository root)
    # Eclipse stores its local workspace cache here when the backend folder is selected as the workspace.
    # It is ignored by Git and is not part of the Maven project.
    allowed_top = {'pom.xml', 'README.md', '.gitignore', '.mvn', '.metadata', 'config', 'target'} | set(modules)
    for entry in sorted(os.listdir(BACKEND)):
        if entry not in allowed_top:
            bad('M0', BACKEND + '/' + entry, 'not part of the Maven project (move tooling to tools/)')
    for extra in ('config', '.mvn'):
        for dirpath, dirs, files in os.walk(BACKEND + '/' + extra):
            for f in files:
                if not f.endswith(('.xml', '.config', '.properties')):
                    bad('M0', dirpath + '/' + f, 'not build configuration')
    for m in modules:
        for entry in sorted(os.listdir(BACKEND + '/' + m)):
            if entry not in ({'pom.xml', 'src', 'target', 'README.md'} | ECLIPSE_MODULE_FILES):
                bad('M0', '%s/%s/%s' % (BACKEND, m, entry), 'not part of the Maven module')
        for dirpath, dirs, files in os.walk(BACKEND + '/' + m + '/src'):
            for f in files:
                if f.endswith(('.py', '.sh', '.js', '.mjs', '.ts', '.rb', '.pl', '.bat', '.ps1')):
                    bad('M0', dirpath + '/' + f, 'script inside a Maven module')

    def r1(f, sql):
        """R1: every tenant table the statement reads or changes is filtered by `tenant_id = :tenantId` at the
        statement's own level (each FROM/JOIN table, outside sub-selects and OR branches): the generator's parser
        must find nothing to add."""
        if not re.search(r'\b(SELECT|UPDATE|DELETE)\b', sql) or not any(t in sql.lower() for t in tenant_tables):
            return
        try:
            if tenant_sql.add_tenant(sql, tenant_tables) != sql:
                bad('R1', f, 'missing tenant predicate: ' + ' '.join(sql.split())[:90])
        except tenant_sql.Unsupported as e:
            if os.path.basename(f) not in R1_FRAGMENTS:
                bad('R1', f, '%s: %s' % (e, ' '.join(sql.split())[:70]))

    for m in modules:
        if re.search(ORM_ARTIFACTS, open('%s/%s/pom.xml' % (BACKEND, m)).read()):
            bad('B1', '%s/%s/pom.xml' % (BACKEND, m), 'ORM dependency')
        for root in ('main', 'test', 'component-test'):
            for f in java(m, root):
                s = open(f).read()
                code = re.sub(r'/\*.*?\*/|//[^\n]*', '', s, flags=re.S)
                if re.search(r'\bTransactional\b|\bTransactionTemplate\b', code):
                    bad('B1', f, '@Transactional / TransactionTemplate')
                if re.search(r'import (jakarta\.persistence|javax\.persistence|org\.hibernate|org\.springframework'
                             r'\.data)\.', code):
                    bad('B1', f, 'ORM import')
                if re.search(r'import com\.tngtech\.archunit|import org\.springframework\.modulith', code):
                    bad('B1', f, 'ArchUnit / Modulith')
                if root != 'test' and re.search(MOCKS, code):
                    bad('T', f, 'mock or stub outside a unit test')
        for f in java(m):
            s = open(f).read()
            normalized = f.replace('\\', '/')
            layer = normalized.split('/com/mysms/%s/' % m)[-1].split('/')[0] if m not in SHARED else None
            for q in re.findall(r'"""(.*?)"""', s, re.S):
                text = ' '.join(q.split())
                if not re.search(r'\b(SELECT|UPDATE|DELETE)\b', text):
                    continue
                if layer and layer not in ('repository',) and re.search(r'\b(FROM|UPDATE)\s+[a-z]+\.[a-z_]+', text):
                    bad('R2', f, 'SQL outside the repository layer')
                r1(f, q)
                for target in re.findall(r'\bDELETE\s+FROM\s+(\S+)', text, re.I):
                    # module-local infrastructure is pruned by design (database/README "Events", api/README
                    # "Idempotency"); IdempotencyStore names its table through {table} = <module>.idempotency_key
                    infra = re.search(r'\.(idempotency_key|outbox_event|inbox_event)$', target) or (
                        target == '{table}' and os.path.basename(f) == 'IdempotencyStore.java')
                    if not infra:
                        bad('§6', f, 'hard DELETE: ' + text[:70])
            for lit in re.findall(r'"((?:SELECT|UPDATE|DELETE) [^"]*)"', s):
                r1(f, lit)
    for m in domain:
        pom = open('%s/%s/pom.xml' % (BACKEND, m)).read()
        for block in re.findall(r'<dependency>(.*?)</dependency>', pom, re.S):
            name = re.search(r'<groupId>com\.mysms</groupId>\s*<artifactId>([\w-]+)</artifactId>', block)
            if not name:
                continue
            d = name.group(1)
            # the event bus implementation is a runtime dependency only: no module code can import it
            runtime_bus = d == 'messaging' and '<scope>runtime</scope>' in block
            if d not in ('core', 'contracts', 'sdk', 'auth-client', 'mysms') and not runtime_bus:
                bad('A1', '%s/%s/pom.xml' % (BACKEND, m), 'depends on ' + d)
        for f in glob.glob('%s/%s/src/main/java/**/*.java' % (BACKEND, m), recursive=True):
            if re.search(r'^import (com\.rabbitmq|com\.mysms\.messaging)\.', open(f).read(), re.M):
                bad('A1', f, 'imports the bus implementation (use core.events)')
        for f in java(m):
            rel = f.replace('\\', '/').split('/com/mysms/%s/' % m)[-1]
            if '/' in rel and rel.split('/')[0] not in PACKAGES:
                bad('§3', f, 'package %s is not a layer role' % rel.split('/')[0])
        for f in glob.glob('%s/%s/src/main/java/com/mysms/%s/domain/*Entity.java' % (BACKEND, m, m)):
            s = open(f).read()
            if 'extends BaseEntity' not in s:
                bad('§5', f, 'entity does not extend BaseEntity')
            if 'TABLE = "%s.' % m not in s:
                bad('§5', f, 'entity names no TABLE of its schema')
        if not glob.glob('%s/%s/src/test/java/**/ModuleBoundaryTest.java' % (BACKEND, m), recursive=True):
            bad('T', '%s/%s' % (BACKEND, m), 'no ModuleBoundaryTest')
        if not glob.glob('%s/%s/src/component-test/java/**/*ServiceComponentTest.java' % (BACKEND, m), recursive=True):
            bad('T', '%s/%s' % (BACKEND, m), 'no service component test')
        for f in glob.glob('%s/database/migrations/%s/**/*.sql' % (REPO, m), recursive=True):
            sql = re.sub(r"'(?:[^']|'')*'", "''", re.sub(r'--[^\n]*', '', open(f).read())).replace('"', '')
            named = re.findall(r'\b(?:TABLE(?:\s+ONLY)?|INTO|UPDATE|REFERENCES|ON)\s+([A-Za-z_]+)\.[A-Za-z_]+', sql, re.I)
            named += re.findall(r'\b(?:ON\s+SCHEMA|CREATE\s+SCHEMA(?:\s+IF\s+NOT\s+EXISTS)?)\s+([A-Za-z_]+)', sql, re.I)
            for other in set(x.lower() for x in named):
                if other not in (m, 'pg_catalog', 'public'):
                    bad('§14', f, 'touches schema ' + other)

    # ---- rules of JAVA-CODING-RULES §5-§9, §11, §16 (N1, R5, C3, M1, D2, E2, X1) and §12 (T) ----
    base = open(BACKEND + '/core/src/main/java/com/mysms/core/entity/BaseEntity.java').read()
    fields = re.findall(r'^    private (?:final )?[\w<>, ]+ (\w+);', base, re.M)
    if fields != AUDIT_SET:
        bad('N1', BACKEND + '/core/src/main/java/com/mysms/core/entity/BaseEntity.java',
            'the audit set must be exactly %s, found %s' % (AUDIT_SET, fields))
    if 'UuidV7.next' not in base or 'OffsetDateTime deletedAt' not in base:
        bad('N1', BACKEND + '/core/src/main/java/com/mysms/core/entity/BaseEntity.java',
            'ids must be UUIDv7 and deletedAt a nullable OffsetDateTime')
    for m in modules:
        for f in java(m):
            src = open(f).read()
            rel = os.path.relpath(f, BACKEND).replace('\\', '/')
            if m != 'auth-client':
                if re.search(TOKEN_LIBS, src, re.M) and rel not in TOKEN_ISSUERS:
                    bad('X1', f, 'imports a token library (only auth-client verifies tokens)')
                if re.search(PASSWORD_HASHING, src, re.M) and m != 'iam':
                    bad('X1', f, 'password hashing outside iam')
                if re.search(r'getHeader\(\s*("Authorization"|HttpHeaders\.AUTHORIZATION)', src):
                    bad('X1', f, 'reads the Authorization header (only auth-client does)')
            normalized = f.replace('\\', '/')
            if re.search(r'\.on(Creating|Updating|Deleting)Data\(', src) and '/business/' not in normalized \
                    and not normalized.endswith('/core/entity/BaseEntity.java'):
                bad('N1', f, 'lifecycle hook called outside a business service (§5.8)')
    for m in domain:
        pkg = '%s/%s/src/main/java/com/mysms/%s/' % (BACKEND, m, m)
        mcls = m[0].upper() + m[1:]
        for f in java(m):
            src = open(f).read()
            if os.path.relpath(f, BACKEND).replace('\\', '/') in INFRASTRUCTURE_CONTROLLERS:
                continue
            if re.search(r'@(?:[\w.]+\.)?(Request|Get|Post|Put|Patch|Delete)Mapping\b', src):
                bad('C3', f, 'declares a mapping (controllers implement the generated interfaces)')
            if re.search(r'^import (io\.swagger|org\.springdoc)\.', src, re.M):
                bad('C3', f, 'springdoc/Swagger annotations (the contract is written first, §7.1)')
            if re.search(r'@(?:[\w.]+\.)?(Rest)?ControllerAdvice\b', src):
                bad('E2', f, 'own exception handler (core GlobalExceptionHandler is the only one)')
            if re.search(r'class \w*Exception extends (RuntimeException|Exception|IllegalStateException)\b', src) \
                    or re.search(r'\bBusinessException\b', src):
                bad('E2', f, 'own exception type (use core\'s typed exceptions, §11.3)')
            if re.search(r'new (?:[\w.]+\.)?\w+Exception\([^;]*\b(e|ex|exception|failure|cause)\.getMessage\(\)', src):
                bad('E2', f, 'passes an exception message to the client (§11.2)')
            if 'extends OncePerRequestFilter' in src:
                bad('X1', f, 'a servlet filter in a domain module (auth and idempotency filters live in auth-client/core)')
            if re.search(r'class \w+(DTO|Dto)\b', src):
                bad('D2', f, 'hand-written transport DTO (DTOs are generated from the contract)')
            if re.search(r'@(?:[\w.]+\.)?JsonView\b|ResponseBodyAdvice', src):
                bad('D2', f, 'response shaping outside the read mask (§7.5)')
            if '/model/' in f and re.search(r'private (final )?(java\.util\.)?Optional<', src):
                bad('D2', f, 'Optional field in a model class (partial updates use updateMask, §7.2)')
        for f in glob.glob(pkg + 'domain/*.java'):
            src = open(f).read()
            if not re.search(r'class \w+Entity extends BaseEntity', src):
                bad('N1', f, 'not an entity: a projection is a record in repository/ (§5.4)')
                continue
            head = src[:src.index('public class')]
            annotations = [l.strip() for l in head.split('\n') if l.startswith('@')]
            if annotations != ENTITY_LOMBOK:
                bad('N1', f, 'entity annotations must be exactly %s' % ' '.join(ENTITY_LOMBOK))
            if re.search(r'@(?:[\w.]+\.)?(Data|AllArgsConstructor|ToString|Json\w+|Table|Column|Id|Transient)\b', src):
                bad('N1', f, 'Lombok/Jackson/mapping annotation not allowed on an entity (§5.2, §5.3)')
            if re.search(r'private [\w<>, ]*Map<String, ?(Object|Serializable)>', src) or 'LocalDateTime' in src:
                bad('N1', f, 'Map column or LocalDateTime field (§5.1, §5.5)')
        for f in glob.glob(pkg + 'repository/*.java'):
            src = open(f).read()
            if re.search(r'^public interface\b', src, re.M):
                bad('R5', f, 'repository interface (repositories are plain classes, §6)')
            if re.search(r'String\.format\(|\.formatted\(', src):
                bad('R5', f, 'SQL built with format (constants only, §6)')
            if re.search(r'\bOFFSET\b', src):
                bad('R5', f, 'OFFSET paging (keyset only, §6)')
        for f in glob.glob(pkg + 'controller/*.java'):
            src = open(f).read()
            if os.path.relpath(f, BACKEND).replace('\\', '/') in INFRASTRUCTURE_CONTROLLERS:
                continue
            if not re.search(r'class \w+Controller implements \w+Api\b', src):
                bad('C3', f, 'does not implement a generated contract interface')
            if re.search(r'^import com\.mysms\.%s\.repository\.|^import com\.mysms\.core\.outbox\.|'
                         r'ApplicationEventPublisher' % m, src, re.M):
                bad('C3', f, 'side effects or data access in a controller (§9)')
            for name, body in re.findall(r'public ResponseEntity<Void> (delete\w*)\([^)]*\) \{(.*?)\n    \}', src, re.S):
                if 'noContent()' not in body and 'NotImplementedException' not in body:
                    bad('D2', f, '%s must answer 204 (§7.4)' % name)
        configs = [f for f in glob.glob(pkg + 'mapper/*.java') if '@MapperConfig' in open(f).read()]
        if [os.path.basename(f) for f in configs] != [mcls + 'MapperConfig.java']:
            bad('M1', pkg + 'mapper', 'needs exactly one @MapperConfig, %sMapperConfig' % mcls)
        for f in configs:
            if 'unmappedTargetPolicy = ReportingPolicy.ERROR' not in open(f).read():
                bad('M1', f, 'unmappedTargetPolicy must be ERROR')
        entity_tenant = {os.path.basename(e)[:-5]: 'private UUID tenantId;' in open(e).read()
                         for e in glob.glob(pkg + 'domain/*Entity.java')}
        for f in glob.glob(pkg + 'mapper/*.java'):
            src = open(f).read()
            if '@Mapper' not in src or f in configs:
                continue
            if not re.search(r'@Mapper\(config = %sMapperConfig\.class' % mcls, src):
                bad('M1', f, 'must use config = %sMapperConfig.class' % mcls)
            if re.search(r'componentModel|NullValue(Property|Check)|@(?:[\w.]+\.)?(MappingTarget|Autowired)\b', src) \
                    or re.search(r'abstract class \w+Mapper', src):
                bad('M1', f, 'mapper policy, null strategy, @MappingTarget or state (§8)')
            for match in re.finditer(r'^    (\w+Entity) \w+\(\w+ \w+\);', src, re.M):
                entity = match.group(1)
                start = max(src.rfind(';', 0, match.start()), src.rfind('{', 0, match.start()))
                block = src[start + 1:match.start()]
                ignored = set(re.findall(r'@Mapping\(target = "(\w+)", ignore = true\)', block))
                need = set(AUDIT_SET) | ({'tenantId'} if entity_tenant.get(entity) else set())
                if not need <= ignored:
                    bad('M1', f, '%s from a DTO must ignore %s' % (entity, ', '.join(sorted(need - ignored))))
        if not glob.glob('%s/%s/src/component-test/java/**/*.java' % (BACKEND, m), recursive=True) or not any(
                'TestTokens' in open(t).read()
                for t in glob.glob('%s/%s/src/component-test/java/**/*.java' % (BACKEND, m), recursive=True)):
            bad('T', '%s/%s' % (BACKEND, m), 'component tests sign no real tokens (TestTokens)')
    for m in modules:
        for f in glob.glob('%s/%s/src/test/java/**/*.java' % (BACKEND, m), recursive=True):
            if re.search(r'@(?:[\w.]+\.)?SpringBootTest\b', open(f).read()):
                bad('T', f, 'Spring context in a unit test (component tests live in src/component-test)')
    for f in glob.glob(REPO + '/database/migrations/*/*.sql'):
        sql = re.sub(r"'(?:[^']|'')*'", "''", re.sub(r'--[^\n]*', '', open(f).read()))
        if re.search(r'\bCREATE\s+(OR\s+REPLACE\s+)?(FUNCTION|PROCEDURE)\b', sql):
            bad('R5', f, 'defines a database function or procedure (§6)')
    for f in glob.glob(REPO + '/api/openapi/*.yaml'):
        if f.endswith('mysms-api.yaml'):
            continue  # the bundle repeats the module contracts
        for op in re.findall(r'^    put:\n(?:      .*\n)*?      operationId: ([\w.]+)', open(f).read(), re.M):
            if op not in PUT_ALLOWED:
                bad('D2', f, 'PUT %s (updates are PATCH + updateMask, §7.3)' % op)
    for p in problems:
        print(p)
    print('%d violation(s)' % len(problems))
    sys.exit(1 if problems else 0)


if __name__ == '__main__':
    main()
