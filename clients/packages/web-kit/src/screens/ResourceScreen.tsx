import {
  buildPath,
  can,
  RESOURCES,
  screenById,
  useI18n,
  useSession,
  type PermissionCode,
  type RecordRow,
} from '@mysms/core';
import { Card } from '@mysms/ui-web';
import { lazy, Suspense, useState, type ComponentType } from 'react';
import { CycleRowTimeline } from './admission/CycleTimeline';
import { TakeAttendanceButton } from './attendance/TakeAttendanceButton';
import type * as WorkflowTools from './assessment/AssessmentWorkflowTools';
import { RecordsOf } from './RecordsScreen';
import { defaultColumns, defaultFilter, screenTitleKey } from './resourceColumns';
import type { ResourceScreenDef } from './resourceScreens';

type RowTool = ComponentType<{ row: RecordRow }>;

/**
 * An assessment workflow tool, loaded with the first list that shows one: the tools are a chunk of their own, so the
 * web's initial JS stays within its budget (ui/05 §10).
 */
function workflowTool(name: keyof typeof WorkflowTools): RowTool {
  return lazy(() =>
    import('./assessment/AssessmentWorkflowTools').then((tools) => ({ default: tools[name] })),
  );
}

/** What a row of a list can start besides its editor, by the name a screen definition gives it. */
const ROW_TOOLS: Readonly<Record<NonNullable<ResourceScreenDef['tool']>, RowTool>> = {
  takeAttendance: TakeAttendanceButton,
  cycleTimeline: CycleRowTimeline,
  homeworkBoard: workflowTool('HomeworkBoardTools'),
  homeworkApproval: workflowTool('HomeworkApprovalTools'),
  assignment: workflowTool('AssignmentTools'),
  examSession: workflowTool('ExamSessionTools'),
  exam: workflowTool('ExamTools'),
  marksheet: workflowTool('MarksheetTools'),
  generateResults: workflowTool('GenerateResultsTool'),
  clearDemoData: lazy(() =>
    import('./platform/PlatformTools').then((tools) => ({ default: tools.ClearDemoDataTool })),
  ),
};

/**
 * One inventory screen over one record type: its own heading and route, the columns that identify a row, a status
 * filter, create/edit/delete and the state transitions the role may use. The server decides which rows the caller
 * sees (institution scope, tenant-level records); a role without the list permission is told so, never shown a
 * broken list.
 */
export function ResourceScreen({ screen }: { screen: ResourceScreenDef }) {
  const { t } = useI18n();
  const permissions = useSession((s) => s.me?.permissions);
  const def = RESOURCES.find((r) => r.key === screen.resource);
  const [parentId, setParentId] = useState('');
  const [search, setSearch] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const [editing, setEditing] = useState<RecordRow | null>(null);
  const [creating, setCreating] = useState(false);
  const title = t(screenTitleKey(screen.id));
  const listPermission = def?.ops.List?.permission;
  if (!def || !listPermission || !can(permissions, listPermission as PermissionCode)) {
    return (
      <Card title={title}>
        <p className="hint">{t('adm.resource.noAccess')}</p>
      </Card>
    );
  }
  const Tool = screen.tool ? ROW_TOOLS[screen.tool] : undefined;
  // the detail screen's route has one parameter: the row's id
  const detailRoute = screen.detail ? screenById(screen.detail)?.routes[0] : undefined;
  const detailParam = detailRoute ? /\{([A-Za-z][A-Za-z0-9]*)\}/.exec(detailRoute)?.[1] : undefined;
  return (
    <RecordsOf
      def={def}
      rowLink={
        detailRoute && detailParam
          ? (row) => `/app${buildPath(detailRoute, { [detailParam]: row.id })}`
          : undefined
      }
      rowTool={
        Tool
          ? (row) => (
              <Suspense fallback={null}>
                <Tool row={row} />
              </Suspense>
            )
          : undefined
      }
      title={title}
      intro={t(def.scope === 'global' ? 'adm.resource.introGlobal' : 'adm.resource.intro', {
        label: def.label,
      })}
      columns={screen.columns ?? defaultColumns(def)}
      filterField={screen.filter ?? defaultFilter(def)}
      parentId={parentId}
      setParentId={setParentId}
      search={search}
      setSearch={setSearch}
      notice={notice}
      setNotice={setNotice}
      editing={editing}
      setEditing={setEditing}
      creating={creating}
      setCreating={setCreating}
    />
  );
}
