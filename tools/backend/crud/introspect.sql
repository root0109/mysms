SELECT json_object_agg(t.fq, t.j) FROM (
 SELECT n.nspname||'.'||c.relname AS fq, json_build_object(
  'partitioned', c.relkind='p',
  'cols', (SELECT json_agg(json_build_object('name',a.attname,'type',format_type(a.atttypid,a.atttypmod),'base',ty.typname,
        'nn',a.attnotnull,'default',pg_get_expr(d.adbin,d.adrelid),'generated',a.attgenerated<>'','identity',a.attidentity<>'') ORDER BY a.attnum)
     FROM pg_attribute a JOIN pg_type ty ON ty.oid=a.atttypid LEFT JOIN pg_attrdef d ON d.adrelid=a.attrelid AND d.adnum=a.attnum
     WHERE a.attrelid=c.oid AND a.attnum>0 AND NOT a.attisdropped),
  'cons', (SELECT json_agg(json_build_object('name',co.conname,'type',co.contype,
        'cols',(SELECT json_agg(a.attname ORDER BY k.i) FROM unnest(co.conkey) WITH ORDINALITY k(n,i) JOIN pg_attribute a ON a.attrelid=c.oid AND a.attnum=k.n),
        'ref', CASE WHEN co.contype='f' THEN (SELECT rn.nspname||'.'||rc.relname FROM pg_class rc JOIN pg_namespace rn ON rn.oid=rc.relnamespace WHERE rc.oid=co.confrelid) END,
        'refcols', CASE WHEN co.contype='f' THEN (SELECT json_agg(a.attname ORDER BY k.i) FROM unnest(co.confkey) WITH ORDINALITY k(n,i) JOIN pg_attribute a ON a.attrelid=co.confrelid AND a.attnum=k.n) END,
        'ondelete', co.confdeltype, 'def', pg_get_constraintdef(co.oid)))
     FROM pg_constraint co WHERE co.conrelid=c.oid),
  'uidx', (SELECT json_agg(json_build_object('name',ic.relname,'def',pg_get_indexdef(i.indexrelid),'pred',pg_get_expr(i.indpred,i.indrelid),
        'cols',(SELECT json_agg(CASE WHEN k.n=0 THEN NULL ELSE (SELECT attname FROM pg_attribute WHERE attrelid=c.oid AND attnum=k.n) END ORDER BY k.i) FROM unnest(i.indkey::int2[]) WITH ORDINALITY k(n,i))))
     FROM pg_index i JOIN pg_class ic ON ic.oid=i.indexrelid WHERE i.indrelid=c.oid AND i.indisunique AND NOT i.indisprimary
       AND NOT EXISTS (SELECT 1 FROM pg_constraint x WHERE x.conindid=i.indexrelid)),
  'rls', c.relrowsecurity
 ) AS j
 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
 WHERE c.relkind IN ('r','p') AND NOT c.relispartition AND n.nspname NOT IN ('pg_catalog','information_schema')
) t;
