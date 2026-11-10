-- Homework approvals are written by the homework workflow alone (publish, approve, reject): the contract no longer
-- has a create, update or delete for them (owner decision of 2026-11-10), so their three permissions leave the
-- catalogue. Grants of them to roles and users go with them (fk_role_permission_permission_id and
-- fk_user_permission_permission_id cascade). Reading approvals (assessment.homework_approval.read) stays.
DELETE FROM iam.permission
 WHERE code IN ('assessment.homework_approval.create', 'assessment.homework_approval.update',
                'assessment.homework_approval.delete');
