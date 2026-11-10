-- Homework review (flow F5: assessment.PublishHomework / ApproveHomework / RejectHomework).
-- Homework a teacher may not publish alone waits for a reviewer: the state "pending" between draft and published.
-- A homework has at most one open review at a time, and the review queue is read by state.

ALTER TABLE assessment.homework DROP CONSTRAINT ck_homework_status;
ALTER TABLE assessment.homework ADD CONSTRAINT ck_homework_status CHECK (status IN ('draft', 'pending', 'published'));
COMMENT ON COLUMN assessment.homework.status IS 'Publication state: draft, pending (waiting for review) or published.';

CREATE UNIQUE INDEX uq_homework_approval_pending ON assessment.homework_approval (homework_id) WHERE decision = 'pending' AND deleted_at IS NULL;
CREATE INDEX ix_homework_pending ON assessment.homework (tenant_id, institution_id) WHERE status = 'pending' AND deleted_at IS NULL;
