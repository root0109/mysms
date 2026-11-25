import { json, type Handler } from './fakeApi';
import { fakeRecords } from './records';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;

/**
 * People and their links as the student-office screens see them: two students and a guardian (Priya, Arjun and
 * their mother Meera Sharma), a relationship type, the guardian without a login account yet, and a certificate kept
 * for Priya.
 */
export const peopleFixture = {
  priyaPersonId: id('d01'),
  arjunPersonId: id('d02'),
  meeraPersonId: id('d03'),
  priyaId: id('d11'),
  arjunId: id('d12'),
  guardianId: id('d21'),
  motherTypeId: id('d31'),
  fileId: id('d41'),
} as const;

/** What the student office needs for guardians and documents (SIS-04, SIS-08). */
export const PEOPLE_OFFICE = [
  'people.person.read',
  'sis.student.read',
  'sis.guardian.read',
  'sis.guardian.update',
  'sis.guardian.create_portal_account',
  'sis.student_guardian.create',
  'sis.relationship_type.read',
  'people.person_document.read',
  'people.person_document.create',
  'platform.file_object.create_upload_url',
  'platform.file_object.complete_upload',
  'platform.file_object.get_download_url',
] as const;

/** Persons, students, guardians, links and documents over the generic record handlers, and a file upload. */
export function fakePeople(): Record<string, Handler> {
  const f = peopleFixture;
  return {
    ...fakeRecords({
      collection: '/v1/people/persons',
      item: '/v1/people/persons/{personId}',
      required: ['displayName'],
      rows: [
        { id: f.priyaPersonId, displayName: 'Priya Sharma' },
        { id: f.arjunPersonId, displayName: 'Arjun Sharma' },
        { id: f.meeraPersonId, displayName: 'Meera Sharma' },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/sis/students',
      item: '/v1/sis/students/{studentId}',
      required: ['personId', 'institutionId'],
      rows: [
        { id: f.priyaId, personId: f.priyaPersonId, admissionNumber: 'A-101', status: 'active' },
        { id: f.arjunId, personId: f.arjunPersonId, admissionNumber: 'A-102', status: 'active' },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/sis/guardians',
      item: '/v1/sis/guardians/{guardianId}',
      required: ['personId'],
      rows: [
        { id: f.guardianId, personId: f.meeraPersonId, profession: 'teacher', mobile: '+91 98450 00000' },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/sis/students/{studentId}/guardians',
      item: '/v1/sis/students/{studentId}/guardians/{studentGuardianId}',
      required: ['guardianId'],
      rows: [],
    }),
    ...fakeRecords({
      collection: '/v1/sis/relationship-types',
      item: '/v1/sis/relationship-types/{relationshipTypeId}',
      required: ['name'],
      rows: [
        { id: f.motherTypeId, name: 'Mother', isActive: true },
        { id: id('d32'), name: 'Retired', isActive: false },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/people/persons/{personId}/documents',
      item: '/v1/people/persons/{personId}/documents/{personDocumentId}',
      required: ['documentType', 'name', 'fileId'],
      rows: [
        {
          id: id('d51'),
          personId: f.priyaPersonId,
          documentType: 'certificate',
          name: 'Birth certificate',
          fileId: id('d52'),
          status: 'active',
        },
      ],
    }),
    'POST /v1/sis/guardians/{guardianId}:createPortalAccount': (_req, p) =>
      json({ id: p.guardianId, personId: f.meeraPersonId, userId: id('d61'), etag: '"2"' }),
    'POST /v1/platform/files:createUploadUrl': () =>
      json({
        fileId: f.fileId,
        uploadUrl: `https://files.example/upload/${f.fileId}?X-Goog-Signature=1`,
        uploadHeaders: { 'Content-Type': 'application/pdf' },
        expiresAt: '2026-11-11T10:15:00Z',
      }),
    'POST /v1/platform/files/{fileObjectId}:complete': (_req, { fileObjectId }) =>
      json({ id: fileObjectId, fileName: 'tc.pdf', sizeBytes: '3', status: 'available', etag: '"2"' }),
    'GET /v1/platform/files/{fileObjectId}:downloadUrl': (_req, { fileObjectId }) =>
      json({
        url: `https://files.example/${fileObjectId}?X-Goog-Signature=1`,
        expiresAt: '2026-11-11T10:10:00Z',
      }),
  };
}
