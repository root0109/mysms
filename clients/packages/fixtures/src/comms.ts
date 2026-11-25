import { json, problem, type Handler } from './fakeApi';
import { onePage, pageOf } from './paging';
import { fakeRecords } from './records';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;

/**
 * A school's communication as the comms, library, careers and greetings screens see it: a published notice, a draft
 * and a cohort announcement; the signed-in account's inbox (one unread notification linking to a screen, one read);
 * a calendar week; a conversation with two messages; notification types, a rule, a channel and a template; library
 * cards, loans and titles; job openings with an application; and the birthdays of a week.
 */
export const commsFixture = {
  me: '01960000-0000-7000-8000-000000000002',
  personId: '01960000-0000-7000-8000-0000000000b2',
  institutionId: id('c01'),
  publishedNoticeId: id('c11'),
  draftNoticeId: id('c12'),
  cohortNoticeId: id('c13'),
  cohortId: id('c14'),
  unreadId: id('c21'),
  readId: id('c22'),
  conversationId: id('c31'),
  messageId: id('c33'),
  otherUserId: id('c32'),
  typeId: id('c41'),
  channelId: id('c42'),
  templateId: id('c43'),
  cardId: id('c51'),
  mediaId: id('c52'),
  loanId: id('c53'),
  openingId: id('c61'),
  closedOpeningId: id('c62'),
  applicationId: id('c63'),
  birthdayPersonId: id('c71'),
  taskId: id('c81'),
  studentId: id('c91'),
  routeId: id('ca1'),
  stopId: id('ca2'),
} as const;
const f = commsFixture;

/** What a member of staff needs to read and write notices, conversations and their own inbox and calendar. */
export const COMMS_STAFF = [
  'comms.notice.read',
  'comms.notice.create',
  'comms.notice.update',
  'comms.notice.publish_notice',
  'comms.notice.close_notice_replies',
  'comms.notice_audience.read',
  'comms.notice_version.read',
  'comms.in_app_notification.read',
  'comms.in_app_notification.mark_notification_read',
  'comms.in_app_notification.mark_all_notifications_read',
  'comms.in_app_notification.archive_notifications',
  'comms.event.get_my_calendar',
  'comms.calendar_export.export_calendar',
  'comms.conversation.read',
  'comms.conversation.create',
  'comms.conversation.mark_conversation_read',
  'comms.conversation_message.read',
  'comms.conversation_message.create',
  'comms.conversation_message.hide_message_for_me',
  'comms.conversation_participant.read',
  'comms.notification_type.read',
  'comms.notification_rule.read',
  'comms.notification_channel.read',
  'comms.notification_channel.send_test_message',
  'comms.notification_channel_property.read',
  'comms.message_template.read',
  'comms.post.get_feed',
  'comms.task.read',
  'comms.task.claim_task',
  'people.person.list_birthdays',
  'people.person.send_greetings',
  'org.institution.read',
] as const;

/** What a library member, a family, a candidate and a poster need for the "my …" views and the careers screens. */
export const SERVICES_MEMBER = [
  'library.library_card.read',
  'library.loan.read',
  'library.media.read',
  'library.media_unit.read',
  'library.reservation.read',
  'careers.job_opening.read',
  'careers.job_opening.create',
  'careers.job_opening.update',
  'careers.job_application.read',
  'careers.job_application.create',
  'careers.job_application.update_application_status',
  'careers.job_application.email_applicant',
  'careers.candidate_preference.read',
  'transport.student_transport.read',
  'transport.route.read',
  'transport.route_stop.read',
] as const;

/** Whether a partitioned list named its time range (the server answers 400 without one). */
const ranged = (req: Request) =>
  (new URL(req.url).searchParams.get('filter') ?? '').includes('createTime >=');

export function fakeComms(): Record<string, Handler> {
  const inbox = [
    {
      id: f.unreadId,
      recipientUserId: f.me,
      title: 'Homework due tomorrow',
      body: 'Fractions worksheet',
      actionUrl: `mysms://comms/notices/${f.publishedNoticeId}`,
      createTime: '2026-03-02T08:00:00Z',
    },
    {
      id: f.readId,
      recipientUserId: f.me,
      title: 'Fee receipt',
      createTime: '2026-03-01T08:00:00Z',
      readAt: '2026-03-01T09:00:00Z',
    },
  ];
  const archived = new Set<string>();
  return {
    ...fakeRecords({
      collection: '/v1/comms/notices',
      item: '/v1/comms/notices/{noticeId}',
      required: ['title', 'body', 'audience'],
      rows: [
        {
          id: f.publishedNoticeId,
          institutionId: f.institutionId,
          audience: 'school',
          title: 'Sports day',
          body: 'Sports day is on Friday.\nWear house colours.',
          isPublished: true,
          isClosed: false,
          allowReplies: true,
          createTime: '2026-03-01T07:00:00Z',
        },
        {
          id: f.draftNoticeId,
          institutionId: f.institutionId,
          audience: 'teachers',
          title: 'Staff meeting',
          body: 'Monday 4 pm',
          isPublished: false,
          isClosed: false,
          createTime: '2026-03-02T07:00:00Z',
        },
        {
          id: f.cohortNoticeId,
          audience: 'cohort',
          cohortId: f.cohortId,
          title: 'Live class moved',
          body: 'Now at 6 pm',
          isPublished: true,
          isClosed: false,
          createTime: '2026-03-02T06:00:00Z',
        },
      ],
    }),
    'POST /v1/comms/notices/{noticeId}:closeReplies': async (req) => {
      const body = (await req.json()) as { etag?: string };
      return body.etag === '"1"'
        ? json({ id: f.publishedNoticeId, isClosed: true })
        : problem(412, 'PRECONDITION_FAILED');
    },
    ...fakeRecords({
      collection: '/v1/comms/notices/{noticeId}/audiences',
      item: '/v1/comms/notices/{noticeId}/audiences/{noticeAudienceId}',
      required: [],
    }),
    ...fakeRecords({
      collection: '/v1/comms/notices/{noticeId}/versions',
      item: '/v1/comms/notices/{noticeId}/versions/{noticeVersionId}',
      required: [],
    }),
    'GET /v1/comms/notifications': (req) => {
      if (!ranged(req)) return problem(400, 'INVALID_FILTER');
      const unread = (new URL(req.url).searchParams.get('filter') ?? '').includes('readAt = null');
      return pageOf(
        req,
        inbox.filter((row) => !archived.has(row.id) && (!unread || !row.readAt)),
      );
    },
    'POST /v1/comms/notifications/{inAppNotificationId}:markRead': (_req, p) => {
      const row = inbox.find((one) => one.id === p.inAppNotificationId);
      if (!row) return problem(404, 'NOT_FOUND');
      row.readAt = '2026-03-02T10:00:00Z';
      return json(row);
    },
    'POST /v1/comms/notifications:markAllRead': () => {
      for (const row of inbox) row.readAt ??= '2026-03-02T10:00:00Z';
      return new Response(null, { status: 204 });
    },
    'POST /v1/comms/notifications:archive': async (req) => {
      const body = (await req.json()) as { notificationIds: string[] };
      for (const one of body.notificationIds) archived.add(one);
      return new Response(null, { status: 204 });
    },
    'GET /v1/comms/me/calendar': (req) => {
      const query = new URL(req.url).searchParams;
      if (!query.get('from') || !query.get('to')) return problem(400, 'INVALID_ARGUMENT');
      return onePage(req, [
        {
          itemType: 'event',
          id: id('c91'),
          title: 'Sports day',
          startAt: '2026-03-06T03:30:00Z',
          allDay: true,
        },
        {
          itemType: 'due_date',
          id: id('c92'),
          title: 'Fractions worksheet',
          startAt: '2026-03-03T12:30:00Z',
        },
        // outside any week the tests show: the screen leaves it out
        { itemType: 'holiday', id: id('c93'), title: 'Holi', startAt: '2026-05-14T00:00:00Z', allDay: true },
      ]);
    },
    'POST /v1/comms/me/calendar:export': () => json({ name: 'operations/x', done: true }, { status: 202 }),
    ...fakeRecords({
      collection: '/v1/comms/conversations',
      item: '/v1/comms/conversations/{conversationId}',
      required: ['conversationType'],
      rows: [
        {
          id: f.conversationId,
          conversationType: 'direct',
          title: 'Diya’s mother',
          isActive: true,
          lastMessageAt: '2026-03-02T08:30:00Z',
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/comms/conversations/{conversationId}/messages',
      item: '/v1/comms/conversations/{conversationId}/messages/{conversationMessageId}',
      required: ['body'],
      fromToken: { senderUserId: f.me },
      rows: [
        {
          id: f.messageId,
          conversationId: f.conversationId,
          senderUserId: f.otherUserId,
          body: 'Is the bus late today?',
          createTime: '2026-03-02T08:00:00Z',
        },
        {
          id: id('c34'),
          conversationId: f.conversationId,
          senderUserId: f.me,
          body: 'Ten minutes, sorry.',
          createTime: '2026-03-02T08:30:00Z',
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/comms/conversations/{conversationId}/participants',
      item: '/v1/comms/conversations/{conversationId}/participants/{conversationParticipantId}',
      required: ['userId'],
    }),
    'POST /v1/comms/conversations/{conversationId}:markRead': () => new Response(null, { status: 204 }),
    'POST /v1/comms/messages/{messageId}:hideForMe': () => new Response(null, { status: 204 }),
    ...fakeRecords({
      collection: '/v1/comms/notification-types',
      item: '/v1/comms/notification-types/{notificationTypeId}',
      required: ['code', 'description', 'moduleCode'],
      rows: [
        {
          id: f.typeId,
          code: 'fee.due',
          description: 'A fee falls due',
          moduleCode: 'finance',
          isEnabled: true,
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/comms/notification-types/{notificationTypeId}/rules',
      item: '/v1/comms/notification-types/{notificationTypeId}/rules/{notificationRuleId}',
      required: ['roleId'],
      rows: [
        {
          id: id('c44'),
          notificationTypeId: f.typeId,
          roleId: id('c45'),
          isEnabled: true,
          emailSubjectTemplate: 'Fee due',
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/comms/notification-channels',
      item: '/v1/comms/notification-channels/{notificationChannelId}',
      required: ['code'],
      rows: [{ id: f.channelId, code: 'email', description: 'School e-mail', isEnabled: true }],
    }),
    ...fakeRecords({
      collection: '/v1/comms/notification-channels/{notificationChannelId}/properties',
      item: '/v1/comms/notification-channels/{notificationChannelId}/properties/{notificationChannelPropertyId}',
      required: ['propertyKey'],
      rows: [
        {
          id: id('c46'),
          notificationChannelId: f.channelId,
          propertyKey: 'smtp.password',
          secretValueMasked: '••••',
        },
      ],
    }),
    'POST /v1/comms/notification-channels/{notificationChannelId}:sendTest': async (req) => {
      const body = (await req.json()) as { to?: string };
      return body.to === 'ravi@school.example'
        ? new Response(null, { status: 204 })
        : problem(422, 'NOT_OWN_ADDRESS');
    },
    ...fakeRecords({
      collection: '/v1/comms/message-templates',
      item: '/v1/comms/message-templates/{messageTemplateId}',
      required: ['channel', 'name'],
      rows: [
        { id: f.templateId, channel: 'email', name: 'Birthday wishes', code: 'birthday', isActive: true },
      ],
    }),
    'GET /v1/comms/posts:feed': (req) =>
      onePage(req, [
        {
          id: id('c82'),
          institutionId: f.institutionId,
          academicYearId: id('c83'),
          authorUserId: f.otherUserId,
          body: 'Science fair photos',
          postedAt: '2026-03-02T05:00:00Z',
        },
      ]),
    'POST /v1/comms/tasks/{taskId}:claim': (_req, p) =>
      p.taskId === f.taskId
        ? json({ items: [{ taskId: f.taskId, claimedByUserId: f.me }] })
        : problem(404, 'NOT_FOUND'),
    'GET /v1/people/persons:birthdays': (req) => {
      const query = new URL(req.url).searchParams;
      if (query.get('institutionId') !== f.institutionId) return problem(404, 'NOT_FOUND');
      const kinds = query.getAll('kinds');
      const rows = [
        {
          personId: f.birthdayPersonId,
          displayName: 'Diya Menon',
          date: '2026-03-04',
          kind: 'birthday',
          role: 'student',
          years: 11,
        },
        {
          personId: id('c72'),
          displayName: 'Anil Rao',
          date: '2026-03-05',
          kind: 'work_anniversary',
          role: 'staff',
          years: 5,
        },
      ];
      return onePage(
        req,
        rows.filter((row) => kinds.length === 0 || kinds.includes(row.kind)),
      );
    },
    'POST /v1/people/persons:sendGreetings': async (req) => {
      const body = (await req.json()) as { personIds: string[]; templateId: string; channel: string };
      if (body.templateId !== f.templateId) return problem(422, 'VALIDATION_FAILED');
      return json({ queued: body.personIds.length, skipped: 0 });
    },
  };
}

/** The library and careers as a member and a candidate see them. */
export function fakeServices(): Record<string, Handler> {
  return {
    ...fakeRecords({
      collection: '/v1/library/library-cards',
      item: '/v1/library/library-cards/{libraryCardId}',
      required: [],
      rows: [
        { id: f.cardId, personId: f.personId, holderType: 'staff', cardNumber: 'L-1', status: 'active' },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/library/loans',
      item: '/v1/library/loans/{loanId}',
      required: [],
      rows: [
        {
          id: f.loanId,
          libraryCardId: f.cardId,
          mediaId: f.mediaId,
          mediaUnitId: id('c54'),
          issuedOn: '2026-02-01',
          dueOn: '2026-02-15',
          status: 'issued',
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/library/media',
      item: '/v1/library/media/{mediaId}',
      required: ['title'],
      rows: [{ id: f.mediaId, institutionId: f.institutionId, title: 'Wings of Fire', isActive: true }],
    }),
    ...fakeRecords({
      collection: '/v1/library/media/{mediaId}/units',
      item: '/v1/library/media/{mediaId}/units/{mediaUnitId}',
      required: ['accessionNumber'],
      rows: [{ id: id('c54'), mediaId: f.mediaId, accessionNumber: 'A-0001', status: 'issued' }],
    }),
    ...fakeRecords({
      collection: '/v1/library/reservations',
      item: '/v1/library/reservations/{reservationId}',
      required: ['mediaId', 'dateFrom', 'dateTo'],
      fromToken: { requestedByUserId: f.me },
    }),
    ...fakeRecords({
      collection: '/v1/careers/job-openings',
      item: '/v1/careers/job-openings/{jobOpeningId}',
      required: ['title'],
      rows: [
        {
          id: f.openingId,
          postedByUserId: f.me,
          title: 'Mathematics teacher',
          companyName: 'Main School',
          location: 'Pune',
          employmentType: 'full_time',
          workMode: 'on_site',
          status: 'open',
          description: 'Grades 6 to 8.',
        },
        {
          id: f.closedOpeningId,
          postedByUserId: f.otherUserId,
          title: 'Librarian',
          companyName: 'Main School',
          location: 'Pune',
          employmentType: 'part_time',
          status: 'closed',
          description: 'Afternoons.',
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/careers/job-openings/{jobOpeningId}/applications',
      item: '/v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}',
      required: ['resumeFileId'],
      rows: [
        {
          id: f.applicationId,
          jobOpeningId: f.closedOpeningId,
          applicantUserId: f.me,
          resumeFileId: id('c64'),
          status: 'shortlisted',
        },
      ],
    }),
    'POST /v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}:setStatus': async (
      req,
      p,
    ) => {
      const body = (await req.json()) as { status: string };
      return json({ id: p.jobApplicationId, jobOpeningId: p.jobOpeningId, status: body.status });
    },
    'POST /v1/careers/job-applications/{applicationId}:email': () => new Response(null, { status: 204 }),
    ...fakeRecords({
      collection: '/v1/transport/student-transports',
      item: '/v1/transport/student-transports/{studentTransportId}',
      required: ['studentId'],
      rows: [
        {
          id: id('ca3'),
          studentId: f.studentId,
          transportMode: 'school_bus',
          routeId: f.routeId,
          routeStopId: f.stopId,
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/transport/routes',
      item: '/v1/transport/routes/{routeId}',
      required: ['name'],
      rows: [
        {
          id: f.routeId,
          name: 'Route 4',
          vehicleNumber: 'MH12 AB 1234',
          startTime: '07:00',
          endTime: '08:00',
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/transport/routes/{routeId}/stops',
      item: '/v1/transport/routes/{routeId}/stops/{routeStopId}',
      required: ['stopName'],
      rows: [
        { id: id('ca4'), routeId: f.routeId, sequence: 1, stopName: 'Depot', pickupTime: '07:00' },
        { id: f.stopId, routeId: f.routeId, sequence: 2, stopName: 'Lake Road', pickupTime: '07:20' },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/careers/candidate-preferences',
      item: '/v1/careers/candidate-preferences/{candidatePreferenceId}',
      required: ['personId'],
    }),
  };
}
