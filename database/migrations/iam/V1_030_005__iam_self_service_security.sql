-- MySMS iam: a user's own security is self-service (ACC-08 Security & sessions). Every role may list and manage its
-- own second factors and see and end its own sign-in sessions; the permissions can no longer be removed from a role.
-- Reaching another user's sessions still needs iam.user_session.read / .delete held tenant-wide (UserSessionRules):
-- a self-service permission is never part of a tenant-wide grant.
UPDATE iam.permission
   SET applies_to_all_roles = true,
       is_modifiable = false
 WHERE code IN ('iam.mfa_credential.read', 'iam.mfa_credential.enroll', 'iam.mfa_credential.disable',
                'iam.mfa_credential.regenerate_recovery_codes', 'iam.user_session.read', 'iam.user_session.delete');
