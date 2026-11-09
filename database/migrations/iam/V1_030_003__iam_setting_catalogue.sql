-- MySMS iam: the product's setting catalogue (ui/04 §9). Product data like the permission catalogue, not tenant
-- reference data. Later catalogue changes are new migrations.
INSERT INTO iam.setting_definition
       (setting_key, category, value_type, allowed_values, min_value, max_value, default_value, allowed_scopes, user_editable, description, sort_order)
VALUES ('ui.theme', 'appearance', 'choice', ARRAY['system', 'light', 'dark', 'high_contrast'], NULL, NULL, 'system',
        ARRAY['tenant', 'institution', 'department', 'class_section', 'user'], true, 'Colour theme of the apps.', 10),
       ('ui.density', 'appearance', 'choice', ARRAY['comfortable', 'compact'], NULL, NULL, 'comfortable',
        ARRAY['tenant', 'institution', 'department', 'class_section', 'user'], true, 'Spacing of tables and lists.', 20),
       ('ui.textScale', 'appearance', 'integer', NULL, 100, 150, '100',
        ARRAY['tenant', 'institution', 'department', 'class_section', 'user'], true, 'Text size in percent.', 30),
       ('ui.reducedMotion', 'appearance', 'boolean', NULL, NULL, NULL, 'false',
        ARRAY['tenant', 'institution', 'department', 'class_section', 'user'], true, 'Reduce animations.', 40),
       ('ui.locale', 'language', 'choice', ARRAY['en', 'hi'], NULL, NULL, 'en',
        ARRAY['tenant', 'institution', 'department', 'class_section', 'user'], true, 'Language of the apps.', 10),
       ('ui.timezone', 'language', 'string', NULL, NULL, NULL, 'Asia/Kolkata',
        ARRAY['tenant', 'institution', 'user'], true, 'Time zone used to show dates and times (IANA name).', 20),
       ('ui.dateFormat', 'language', 'choice', ARRAY['dd-MM-yyyy', 'MM/dd/yyyy', 'yyyy-MM-dd'], NULL, NULL, 'dd-MM-yyyy',
        ARRAY['tenant', 'institution', 'department', 'class_section', 'user'], true, 'How dates are written.', 30),
       ('ui.landingScreen', 'navigation', 'string', NULL, NULL, NULL, '',
        ARRAY['tenant', 'institution', 'department', 'class_section', 'user'], true, 'Screen id to open after sign-in; empty = the role''s home.', 10),
       ('notify.emailDigest', 'notifications', 'choice', ARRAY['off', 'daily', 'weekly'], NULL, NULL, 'daily',
        ARRAY['tenant', 'institution', 'department', 'class_section', 'user'], true, 'Summary e-mail of unread notifications.', 10),
       ('security.sessionTimeoutMinutes', 'security', 'integer', NULL, 5, 480, '60',
        ARRAY['tenant', 'institution'], false, 'Idle minutes before the apps sign out (set by administrators).', 10);
