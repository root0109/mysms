import { useQuery } from '@tanstack/react-query';
import { allPages, pageQuery } from '../api/pages';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';
import { fieldIn, listRecords } from '../records/api';
import { RESOURCES } from '../generated/resources';
import type { ResourceDef } from '../records/types';

/** One subject a class takes and who teaches it (academics class subjects and teaching assignments). */
export interface ClassSubjectLine {
  id: string;
  subjectId: string;
  offeringType?: string;
  isActive: boolean;
  teachers: { staffId: string; periodsPerWeek?: number; teacherRole?: string }[];
}

const def = (key: string) => RESOURCES.find((r) => r.key === key) as ResourceDef;

/**
 * The subjects of a class section with their teachers: academics.ListClassSubjects under the class, then
 * academics.ListTeachingAssignments of those subjects (forty at a time, the filter's limit), active ones only.
 * Names of subjects and teachers are held elsewhere; screens read them with useSubjectNames and useStaffNames.
 * `withTeachers` false (a role that may not read assignments) reads the subjects alone.
 */
export function useClassSubjects(classSectionId: string | null, withTeachers: boolean, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled: enabled && classSectionId !== null,
    queryKey: queryKey('academics', 'class-subjects', { classSectionId, withTeachers }),
    queryFn: async (): Promise<ClassSubjectLine[]> => {
      const subjects = await allPages((page) =>
        listRecords(api, def('academics.ClassSubject'), {
          parentId: classSectionId as string,
          size: 500,
          ...pageQuery(page),
        }),
      );
      const ids = subjects.items.map((row) => row.id);
      const teachersOf = new Map<string, ClassSubjectLine['teachers']>();
      for (let start = 0; withTeachers && start < ids.length; start += 40) {
        const assignments = await allPages((page) =>
          listRecords(api, def('academics.TeachingAssignment'), {
            filter: fieldIn('classSubjectId', ids.slice(start, start + 40)),
            size: 500,
            ...pageQuery(page),
          }),
        );
        for (const row of assignments.items) {
          if (row.isActive === false) continue;
          const key = String(row.classSubjectId);
          teachersOf.set(key, [
            ...(teachersOf.get(key) ?? []),
            {
              staffId: String(row.staffId),
              ...(typeof row.periodsPerWeek === 'number' ? { periodsPerWeek: row.periodsPerWeek } : {}),
              ...(typeof row.teacherRole === 'string' ? { teacherRole: row.teacherRole } : {}),
            },
          ]);
        }
      }
      return subjects.items.map((row) => ({
        id: row.id,
        subjectId: String(row.subjectId),
        ...(typeof row.offeringType === 'string' ? { offeringType: row.offeringType } : {}),
        isActive: row.isActive !== false,
        teachers: teachersOf.get(row.id) ?? [],
      }));
    },
    staleTime: STALE_TIME.list,
  });
}
