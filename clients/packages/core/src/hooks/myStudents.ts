import { useStudentNames } from './attendance';
import { useCan, useSession } from './session';
import { useMyChildren } from './sis';

/** A student whose school life the signed-in account follows: themself, or one of their children. */
export interface MyStudent {
  id: string;
  /** Their name, when the account may read it. */
  name?: string;
}

const NONE: readonly string[] = [];

/**
 * The students the signed-in account reads its own views for ("my loans", "my route", "my attendance"): the
 * account's own student records (iam.GetMe `studentIds`) and, for a guardian, their children (sis.ListMyChildren).
 * The server keeps each read to these students; this only says whose to ask for.
 */
export function useMyStudents() {
  const own = useSession((s) => s.me?.studentIds ?? NONE);
  const mayChildren = useCan('sis.guardian.list_my_children');
  const children = useMyChildren(mayChildren);
  const names = useStudentNames(own);
  const students: MyStudent[] = [
    ...own.map((id) => {
      const name = names.get(id);
      return name ? { id, name } : { id };
    }),
    ...(children.data?.items ?? []).flatMap((card) =>
      card.student.id && !own.includes(card.student.id)
        ? [{ id: card.student.id, name: card.person.displayName }]
        : [],
    ),
  ];
  return {
    students,
    isPending: mayChildren && children.isPending,
    isError: children.isError,
    refetch: () => void children.refetch(),
  };
}
