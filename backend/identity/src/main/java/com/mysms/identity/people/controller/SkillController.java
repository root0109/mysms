package com.mysms.identity.people.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.identity.people.api.SkillApi;
import com.mysms.identity.people.api.model.Skill;
import com.mysms.identity.people.api.model.SkillUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.identity.people.business.SkillService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Skill operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class SkillController implements SkillApi {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @Override
    public ResponseEntity<Skill> createSkill(Skill skill, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Skill created = skillService.createSkill(skill, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/people/skills/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteSkill(UUID skillId, String ifMatch) {
        skillService.deleteSkill(skillId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Skill> getSkill(UUID skillId, String fields) {
        Skill found = skillService.getSkill(skillId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Skill>> listSkills(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Skill> list = PagingUtil.pagingInfo(skillService.listSkills(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Skill> updateSkill(UUID skillId, String ifMatch, SkillUpdate skillUpdate,
            Boolean validateOnly) {
        Skill updated = skillService.updateSkill(skillId, ifMatch, skillUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
