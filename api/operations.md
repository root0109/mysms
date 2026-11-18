# MySMS API operations

All 2055 operations. REST path parameters are UUIDs unless noted. "Public" operations can be called without a token.

## Master Data (`master`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 1 | `GET /v1/master/countries` | `CountryService/ListCountries` | `master.country.read` | Public | List Countries. |
| 2 | `POST /v1/master/countries` | `CountryService/CreateCountry` | `master.country.create` |  | Create Country. |
| 3 | `GET /v1/master/countries/{countryId}` | `CountryService/GetCountry` | `master.country.read` | Public | Get Country. |
| 4 | `PATCH /v1/master/countries/{countryId}` | `CountryService/UpdateCountry` | `master.country.update` |  | Update Country. |
| 5 | `DELETE /v1/master/countries/{countryId}` | `CountryService/DeleteCountry` | `master.country.delete` |  | Delete Country. |
| 6 | `GET /v1/master/states` | `StateProvinceService/ListStateProvinces` | `master.state_province.read` | Public | List StateProvinces. |
| 7 | `POST /v1/master/states` | `StateProvinceService/CreateStateProvince` | `master.state_province.create` |  | Create StateProvince. |
| 8 | `GET /v1/master/states/{stateProvinceId}` | `StateProvinceService/GetStateProvince` | `master.state_province.read` | Public | Get StateProvince. |
| 9 | `PATCH /v1/master/states/{stateProvinceId}` | `StateProvinceService/UpdateStateProvince` | `master.state_province.update` |  | Update StateProvince. |
| 10 | `DELETE /v1/master/states/{stateProvinceId}` | `StateProvinceService/DeleteStateProvince` | `master.state_province.delete` |  | Delete StateProvince. |
| 11 | `GET /v1/master/cities` | `CityService/ListCities` | `master.city.read` | Public | List Cities. |
| 12 | `POST /v1/master/cities` | `CityService/CreateCity` | `master.city.create` |  | Create City. |
| 13 | `GET /v1/master/cities/{cityId}` | `CityService/GetCity` | `master.city.read` | Public | Get City. |
| 14 | `PATCH /v1/master/cities/{cityId}` | `CityService/UpdateCity` | `master.city.update` |  | Update City. |
| 15 | `DELETE /v1/master/cities/{cityId}` | `CityService/DeleteCity` | `master.city.delete` |  | Delete City. |
| 16 | `GET /v1/master/currencies` | `CurrencyService/ListCurrencies` | `master.currency.read` | Public | List Currencies. |
| 17 | `POST /v1/master/currencies` | `CurrencyService/CreateCurrency` | `master.currency.create` |  | Create Currency. |
| 18 | `GET /v1/master/currencies/{currencyId}` | `CurrencyService/GetCurrency` | `master.currency.read` | Public | Get Currency. |
| 19 | `PATCH /v1/master/currencies/{currencyId}` | `CurrencyService/UpdateCurrency` | `master.currency.update` |  | Update Currency. |
| 20 | `DELETE /v1/master/currencies/{currencyId}` | `CurrencyService/DeleteCurrency` | `master.currency.delete` |  | Delete Currency. |

## Platform & Tenancy (`platform`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 21 | `GET /v1/platform/tenants` | `TenantService/ListTenants` | `platform.tenant.read` |  | List Tenants. |
| 22 | `POST /v1/platform/tenants` | `TenantService/CreateTenant` | `platform.tenant.create` |  | Create Tenant. |
| 23 | `GET /v1/platform/tenants/{tenantId}` | `TenantService/GetTenant` | `platform.tenant.read` |  | Get Tenant. |
| 24 | `PATCH /v1/platform/tenants/{tenantId}` | `TenantService/UpdateTenant` | `platform.tenant.update` |  | Update Tenant. |
| 25 | `DELETE /v1/platform/tenants/{tenantId}` | `TenantService/DeleteTenant` | `platform.tenant.delete` |  | Delete Tenant. |
| 26 | `GET /v1/platform/saas-plans` | `SaasPlanService/ListSaasPlans` | `platform.saas_plan.read` | Public | List SaasPlans. |
| 27 | `POST /v1/platform/saas-plans` | `SaasPlanService/CreateSaasPlan` | `platform.saas_plan.create` |  | Create SaasPlan. |
| 28 | `GET /v1/platform/saas-plans/{saasPlanId}` | `SaasPlanService/GetSaasPlan` | `platform.saas_plan.read` | Public | Get SaasPlan. |
| 29 | `PATCH /v1/platform/saas-plans/{saasPlanId}` | `SaasPlanService/UpdateSaasPlan` | `platform.saas_plan.update` |  | Update SaasPlan. |
| 30 | `DELETE /v1/platform/saas-plans/{saasPlanId}` | `SaasPlanService/DeleteSaasPlan` | `platform.saas_plan.delete` |  | Delete SaasPlan. |
| 31 | `GET /v1/platform/tenant-subscriptions` | `TenantSubscriptionService/ListTenantSubscriptions` | `platform.tenant_subscription.read` |  | List TenantSubscriptions. |
| 32 | `GET /v1/platform/tenant-subscriptions/{tenantSubscriptionId}` | `TenantSubscriptionService/GetTenantSubscription` | `platform.tenant_subscription.read` |  | Get TenantSubscription. |
| 33 | `GET /v1/platform/files` | `FileObjectService/ListFileObjects` | `platform.file_object.read` |  | List FileObjects. |
| 34 | `GET /v1/platform/files/{fileObjectId}` | `FileObjectService/GetFileObject` | `platform.file_object.read` |  | Get FileObject. |
| 35 | `DELETE /v1/platform/files/{fileObjectId}` | `FileObjectService/DeleteFileObject` | `platform.file_object.delete` |  | Delete FileObject. |
| 36 | `GET /v1/platform/attachments` | `EntityAttachmentService/ListEntityAttachments` | `platform.entity_attachment.read` |  | List EntityAttachments. |
| 37 | `POST /v1/platform/attachments` | `EntityAttachmentService/CreateEntityAttachment` | `platform.entity_attachment.create` |  | Create EntityAttachment. |
| 38 | `DELETE /v1/platform/attachments/{entityAttachmentId}` | `EntityAttachmentService/DeleteEntityAttachment` | `platform.entity_attachment.delete` |  | Delete EntityAttachment. |
| 39 | `GET /v1/platform/tags` | `TagService/ListTags` | `platform.tag.read` |  | List Tags. |
| 40 | `POST /v1/platform/tags` | `TagService/CreateTag` | `platform.tag.create` |  | Create Tag. |
| 41 | `GET /v1/platform/tags/{tagId}` | `TagService/GetTag` | `platform.tag.read` |  | Get Tag. |
| 42 | `PATCH /v1/platform/tags/{tagId}` | `TagService/UpdateTag` | `platform.tag.update` |  | Update Tag. |
| 43 | `DELETE /v1/platform/tags/{tagId}` | `TagService/DeleteTag` | `platform.tag.delete` |  | Delete Tag. |
| 44 | `GET /v1/platform/tag-assignments` | `TagAssignmentService/ListTagAssignments` | `platform.tag_assignment.read` |  | List TagAssignments. |
| 45 | `POST /v1/platform/tag-assignments` | `TagAssignmentService/CreateTagAssignment` | `platform.tag_assignment.create` |  | Create TagAssignment. |
| 46 | `DELETE /v1/platform/tag-assignments/{tagAssignmentId}` | `TagAssignmentService/DeleteTagAssignment` | `platform.tag_assignment.delete` |  | Delete TagAssignment. |
| 47 | `GET /v1/platform/entity-notes` | `EntityNoteService/ListEntityNotes` | `platform.entity_note.read` |  | List EntityNotes. |
| 48 | `POST /v1/platform/entity-notes` | `EntityNoteService/CreateEntityNote` | `platform.entity_note.create` |  | Create EntityNote. |
| 49 | `GET /v1/platform/entity-notes/{entityNoteId}` | `EntityNoteService/GetEntityNote` | `platform.entity_note.read` |  | Get EntityNote. |
| 50 | `PATCH /v1/platform/entity-notes/{entityNoteId}` | `EntityNoteService/UpdateEntityNote` | `platform.entity_note.update` |  | Update EntityNote. |
| 51 | `DELETE /v1/platform/entity-notes/{entityNoteId}` | `EntityNoteService/DeleteEntityNote` | `platform.entity_note.delete` |  | Delete EntityNote. |
| 52 | `GET /v1/platform/audit-logs` | `AuditLogService/ListAuditLogs` | `platform.audit_log.read` |  | List AuditLogs. |
| 53 | `GET /v1/platform/audit-logs/{auditLogId}` | `AuditLogService/GetAuditLog` | `platform.audit_log.read` |  | Get AuditLog. |
| 54 | `GET /v1/platform/reserved-keywords` | `ReservedKeywordService/ListReservedKeywords` | `platform.reserved_keyword.read` |  | List ReservedKeywords. |
| 55 | `POST /v1/platform/reserved-keywords` | `ReservedKeywordService/CreateReservedKeyword` | `platform.reserved_keyword.create` |  | Create ReservedKeyword. |
| 56 | `GET /v1/platform/reserved-keywords/{reservedKeywordId}` | `ReservedKeywordService/GetReservedKeyword` | `platform.reserved_keyword.read` |  | Get ReservedKeyword. |
| 57 | `PATCH /v1/platform/reserved-keywords/{reservedKeywordId}` | `ReservedKeywordService/UpdateReservedKeyword` | `platform.reserved_keyword.update` |  | Update ReservedKeyword. |
| 58 | `DELETE /v1/platform/reserved-keywords/{reservedKeywordId}` | `ReservedKeywordService/DeleteReservedKeyword` | `platform.reserved_keyword.delete` |  | Delete ReservedKeyword. |
| 59 | `GET /v1/platform/sales-enquiries` | `SalesEnquiryService/ListSalesEnquiries` | `platform.sales_enquiry.read` |  | List SalesEnquiries. |
| 60 | `POST /v1/platform/sales-enquiries` | `SalesEnquiryService/CreateSalesEnquiry` | `platform.sales_enquiry.create` | Public | Create SalesEnquiry. |
| 61 | `GET /v1/platform/sales-enquiries/{salesEnquiryId}` | `SalesEnquiryService/GetSalesEnquiry` | `platform.sales_enquiry.read` |  | Get SalesEnquiry. |
| 62 | `PATCH /v1/platform/sales-enquiries/{salesEnquiryId}` | `SalesEnquiryService/UpdateSalesEnquiry` | `platform.sales_enquiry.update` |  | Update SalesEnquiry. |
| 63 | `DELETE /v1/platform/sales-enquiries/{salesEnquiryId}` | `SalesEnquiryService/DeleteSalesEnquiry` | `platform.sales_enquiry.delete` |  | Delete SalesEnquiry. |
| 64 | `GET /v1/platform/system-settings` | `SystemSettingService/ListSystemSettings` | `platform.system_setting.read` |  | List SystemSettings. |
| 65 | `POST /v1/platform/system-settings` | `SystemSettingService/CreateSystemSetting` | `platform.system_setting.create` |  | Create SystemSetting. |
| 66 | `GET /v1/platform/system-settings/{systemSettingId}` | `SystemSettingService/GetSystemSetting` | `platform.system_setting.read` |  | Get SystemSetting. |
| 67 | `PATCH /v1/platform/system-settings/{systemSettingId}` | `SystemSettingService/UpdateSystemSetting` | `platform.system_setting.update` |  | Update SystemSetting. |
| 68 | `DELETE /v1/platform/system-settings/{systemSettingId}` | `SystemSettingService/DeleteSystemSetting` | `platform.system_setting.delete` |  | Delete SystemSetting. |
| 69 | `GET /v1/platform/themes` | `ThemeService/ListThemes` | `platform.theme.read` | Public | List Themes. |
| 70 | `POST /v1/platform/themes` | `ThemeService/CreateTheme` | `platform.theme.create` |  | Create Theme. |
| 71 | `GET /v1/platform/themes/{themeId}` | `ThemeService/GetTheme` | `platform.theme.read` | Public | Get Theme. |
| 72 | `PATCH /v1/platform/themes/{themeId}` | `ThemeService/UpdateTheme` | `platform.theme.update` |  | Update Theme. |
| 73 | `DELETE /v1/platform/themes/{themeId}` | `ThemeService/DeleteTheme` | `platform.theme.delete` |  | Delete Theme. |
| 74 | `GET /v1/platform/upload-policies` | `UploadPolicyService/ListUploadPolicies` | `platform.upload_policy.read` |  | List UploadPolicies. |
| 75 | `POST /v1/platform/upload-policies` | `UploadPolicyService/CreateUploadPolicy` | `platform.upload_policy.create` |  | Create UploadPolicy. |
| 76 | `GET /v1/platform/upload-policies/{uploadPolicyId}` | `UploadPolicyService/GetUploadPolicy` | `platform.upload_policy.read` |  | Get UploadPolicy. |
| 77 | `PATCH /v1/platform/upload-policies/{uploadPolicyId}` | `UploadPolicyService/UpdateUploadPolicy` | `platform.upload_policy.update` |  | Update UploadPolicy. |
| 78 | `DELETE /v1/platform/upload-policies/{uploadPolicyId}` | `UploadPolicyService/DeleteUploadPolicy` | `platform.upload_policy.delete` |  | Delete UploadPolicy. |
| 79 | `GET /v1/platform/deletion-logs` | `DeletionLogService/ListDeletionLogs` | `platform.deletion_log.read` |  | List DeletionLogs. |
| 80 | `GET /v1/platform/deletion-logs/{deletionLogId}` | `DeletionLogService/GetDeletionLog` | `platform.deletion_log.read` |  | Get DeletionLog. |
| 81 | `GET /v1/platform/storage-quotas` | `StorageQuotaService/ListStorageQuotas` | `platform.storage_quota.read` |  | List StorageQuotas. |
| 82 | `POST /v1/platform/storage-quotas` | `StorageQuotaService/CreateStorageQuota` | `platform.storage_quota.create` |  | Create StorageQuota. |
| 83 | `GET /v1/platform/storage-quotas/{storageQuotaId}` | `StorageQuotaService/GetStorageQuota` | `platform.storage_quota.read` |  | Get StorageQuota. |
| 84 | `PATCH /v1/platform/storage-quotas/{storageQuotaId}` | `StorageQuotaService/UpdateStorageQuota` | `platform.storage_quota.update` |  | Update StorageQuota. |
| 85 | `DELETE /v1/platform/storage-quotas/{storageQuotaId}` | `StorageQuotaService/DeleteStorageQuota` | `platform.storage_quota.delete` |  | Delete StorageQuota. |
| 86 | `POST /v1/platform/files:createUploadUrl` | `FileObjectService/CreateUploadUrl` | `platform.file_object.create_upload_url` |  | Create a pending file and a pre-signed upload URL. |
| 87 | `POST /v1/platform/files/{fileObjectId}:complete` | `FileObjectService/CompleteUpload` | `platform.file_object.complete_upload` |  | Confirm the bytes were uploaded; the file is scanned and becomes available. |
| 88 | `GET /v1/platform/files/{fileObjectId}:downloadUrl` | `FileObjectService/GetDownloadUrl` | `platform.file_object.get_download_url` | Public | Pre-signed download URL. |
| 89 | `GET /v1/platform/branding` | `TenantService/GetBranding` | `platform.tenant.get_branding` | Public | Public branding and locale for login pages and the PWA manifest. |
| 90 | `POST /v1/platform/tenants/{tenantId}:clearDemoData` | `TenantService/ClearDemoData` | `platform.tenant.clear_demo_data` |  | Remove demo data loaded for a trial tenant. |
| 91 | `GET /v1/platform/search` | `TenantService/GlobalSearch` | `platform.tenant.global_search` |  | Permission-aware search across students, staff, guardians, courses, media. |
| 92 | `GET /v1/platform/operations/{operationId}` | `OperationService/GetOperation` | `platform.operation.read` |  | Status of a long-running operation started in this module. |
| 93 | `GET /v1/platform/operations` | `OperationService/ListOperations` | `platform.operation.read` |  | Job centre — the caller's operations across all modules. |
| 94 | `POST /v1/platform/operations/{operationId}:cancel` | `OperationService/CancelOperation` | `platform.operation.cancel_operation` |  | Request cancellation of a running operation. |
| 95 | `GET /v1/platform/reports` | `TenantService/ListReportDefinitions` | `platform.tenant.list_report_definitions` |  | Reports the caller may run. |
| 96 | `POST /v1/platform/reports:run` | `TenantService/RunReport` | `platform.tenant.run_report` |  | Run a report or printable document asynchronously; the operation result holds the output file. |
| 97 | `GET /v1/platform/imports/templates/{entityType}` | `TenantService/GetImportTemplate` | `platform.tenant.get_import_template` |  | Download the CSV/XLSX template for an importable entity. |
| 98 | `POST /v1/platform/imports` | `TenantService/StartImport` | `platform.tenant.start_import` |  | Bulk import from a file; the operation result lists row errors (ImportRowError). |
| 99 | `GET /v1/platform/stock-images` | `TenantService/SearchStockImages` | `platform.tenant.search_stock_images` |  | Search a stock-photo provider for cover images. |
| 100 | `POST /v1/platform/tenant-subscriptions:checkout` | `TenantSubscriptionService/StartSubscriptionCheckout` | `platform.tenant_subscription.start_subscription_checkout` |  | Start paying for a plan. |
| 101 | `POST /v1/platform/tenant-subscriptions/{tenantSubscriptionId}:cancel` | `TenantSubscriptionService/CancelSubscription` | `platform.tenant_subscription.cancel_subscription` |  | Cancel at the end of the paid period. |
| 102 | `POST /v1/platform/tenant-subscriptions/{tenantSubscriptionId}:confirmCheckout` | `TenantSubscriptionService/ConfirmSubscriptionCheckout` | `platform.tenant_subscription.start_subscription_checkout` |  | Confirm a subscription payment with the gateway's checkout signature. |
| 103 | `POST /v1/platform/tenant-subscriptions:webhook` | `TenantSubscriptionService/ReceiveSubscriptionWebhook` | `platform.tenant_subscription.receive_webhook` | Public | Gateway callback for subscription payments, authenticated by the gateway signature. |
| 104 | `POST /v1/platform/sales-enquiries/{salesEnquiryId}:markContacted` | `SalesEnquiryService/MarkEnquiryContacted` | `platform.sales_enquiry.mark_enquiry_contacted` |  | Record follow-up. |
| 105 | `GET /v1/platform/me/storage` | `StorageQuotaService/GetMyStorageUsage` | `platform.storage_quota.get_my_storage_usage` |  | Caller's storage quota and usage. |
| 106 | `POST /v1/platform/files/{fileObjectId}:copy` | `FileObjectService/CopyFile` | `platform.file_object.copy_file` |  | Server-side copy of a stored file into a new file record (no download / upload round trip). |

## Identity & Access (`iam`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 107 | `GET /v1/iam/users` | `UserAccountService/ListUserAccounts` | `iam.user_account.read` |  | List UserAccounts. |
| 108 | `POST /v1/iam/users` | `UserAccountService/CreateUserAccount` | `iam.user_account.create` |  | Create UserAccount. |
| 109 | `GET /v1/iam/users/{userAccountId}` | `UserAccountService/GetUserAccount` | `iam.user_account.read` |  | Get UserAccount. |
| 110 | `PATCH /v1/iam/users/{userAccountId}` | `UserAccountService/UpdateUserAccount` | `iam.user_account.update` |  | Update UserAccount. |
| 111 | `DELETE /v1/iam/users/{userAccountId}` | `UserAccountService/DeleteUserAccount` | `iam.user_account.delete` |  | Delete UserAccount. |
| 112 | `GET /v1/iam/roles` | `RoleService/ListRoles` | `iam.role.read` |  | List Roles. |
| 113 | `POST /v1/iam/roles` | `RoleService/CreateRole` | `iam.role.create` |  | Create Role. |
| 114 | `GET /v1/iam/roles/{roleId}` | `RoleService/GetRole` | `iam.role.read` |  | Get Role. |
| 115 | `PATCH /v1/iam/roles/{roleId}` | `RoleService/UpdateRole` | `iam.role.update` |  | Update Role. |
| 116 | `DELETE /v1/iam/roles/{roleId}` | `RoleService/DeleteRole` | `iam.role.delete` |  | Delete Role. |
| 117 | `GET /v1/iam/permissions` | `PermissionService/ListPermissions` | `iam.permission.read` |  | List Permissions. |
| 118 | `GET /v1/iam/permissions/{permissionId}` | `PermissionService/GetPermission` | `iam.permission.read` |  | Get Permission. |
| 119 | `GET /v1/iam/roles/{roleId}/permissions` | `RoleService/ListRolePermissions` | `iam.role_permission.read` |  | List RolePermissions. |
| 120 | `POST /v1/iam/roles/{roleId}/permissions` | `RoleService/CreateRolePermission` | `iam.role_permission.create` |  | Create RolePermission. |
| 121 | `GET /v1/iam/roles/{roleId}/permissions/{rolePermissionId}` | `RoleService/GetRolePermission` | `iam.role_permission.read` |  | Get RolePermission. |
| 122 | `PATCH /v1/iam/roles/{roleId}/permissions/{rolePermissionId}` | `RoleService/UpdateRolePermission` | `iam.role_permission.update` |  | Update RolePermission. |
| 123 | `DELETE /v1/iam/roles/{roleId}/permissions/{rolePermissionId}` | `RoleService/DeleteRolePermission` | `iam.role_permission.delete` |  | Delete RolePermission. |
| 124 | `GET /v1/iam/users/{userAccountId}/roles` | `UserAccountService/ListUserRoles` | `iam.user_role.read` |  | List UserRoles. |
| 125 | `POST /v1/iam/users/{userAccountId}/roles` | `UserAccountService/CreateUserRole` | `iam.user_role.create` |  | Create UserRole. |
| 126 | `GET /v1/iam/users/{userAccountId}/roles/{userRoleId}` | `UserAccountService/GetUserRole` | `iam.user_role.read` |  | Get UserRole. |
| 127 | `PATCH /v1/iam/users/{userAccountId}/roles/{userRoleId}` | `UserAccountService/UpdateUserRole` | `iam.user_role.update` |  | Update UserRole. |
| 128 | `DELETE /v1/iam/users/{userAccountId}/roles/{userRoleId}` | `UserAccountService/DeleteUserRole` | `iam.user_role.delete` |  | Delete UserRole. |
| 129 | `GET /v1/iam/users/{userAccountId}/permissions` | `UserAccountService/ListUserPermissions` | `iam.user_permission.read` |  | List UserPermissions. |
| 130 | `POST /v1/iam/users/{userAccountId}/permissions` | `UserAccountService/CreateUserPermission` | `iam.user_permission.create` |  | Create UserPermission. |
| 131 | `GET /v1/iam/users/{userAccountId}/permissions/{userPermissionId}` | `UserAccountService/GetUserPermission` | `iam.user_permission.read` |  | Get UserPermission. |
| 132 | `PATCH /v1/iam/users/{userAccountId}/permissions/{userPermissionId}` | `UserAccountService/UpdateUserPermission` | `iam.user_permission.update` |  | Update UserPermission. |
| 133 | `DELETE /v1/iam/users/{userAccountId}/permissions/{userPermissionId}` | `UserAccountService/DeleteUserPermission` | `iam.user_permission.delete` |  | Delete UserPermission. |
| 134 | `GET /v1/iam/users/{userAccountId}/departments` | `UserAccountService/ListUserDepartmentAccesses` | `iam.user_department_access.read` |  | List UserDepartmentAccesses. |
| 135 | `POST /v1/iam/users/{userAccountId}/departments` | `UserAccountService/CreateUserDepartmentAccess` | `iam.user_department_access.create` |  | Create UserDepartmentAccess. |
| 136 | `GET /v1/iam/users/{userAccountId}/departments/{userDepartmentAccessId}` | `UserAccountService/GetUserDepartmentAccess` | `iam.user_department_access.read` |  | Get UserDepartmentAccess. |
| 137 | `PATCH /v1/iam/users/{userAccountId}/departments/{userDepartmentAccessId}` | `UserAccountService/UpdateUserDepartmentAccess` | `iam.user_department_access.update` |  | Update UserDepartmentAccess. |
| 138 | `DELETE /v1/iam/users/{userAccountId}/departments/{userDepartmentAccessId}` | `UserAccountService/DeleteUserDepartmentAccess` | `iam.user_department_access.delete` |  | Delete UserDepartmentAccess. |
| 139 | `GET /v1/iam/users/{userAccountId}/api-tokens` | `UserAccountService/ListAuthTokens` | `iam.auth_token.read` |  | List AuthTokens. |
| 140 | `DELETE /v1/iam/users/{userAccountId}/api-tokens/{authTokenId}` | `UserAccountService/DeleteAuthToken` | `iam.auth_token.delete` |  | Delete AuthToken. |
| 141 | `GET /v1/iam/users/{userAccountId}/devices` | `UserAccountService/ListUserDevices` | `iam.user_device.read` |  | List UserDevices. |
| 142 | `POST /v1/iam/users/{userAccountId}/devices` | `UserAccountService/CreateUserDevice` | `iam.user_device.create` |  | Create UserDevice. |
| 143 | `GET /v1/iam/users/{userAccountId}/devices/{userDeviceId}` | `UserAccountService/GetUserDevice` | `iam.user_device.read` |  | Get UserDevice. |
| 144 | `PATCH /v1/iam/users/{userAccountId}/devices/{userDeviceId}` | `UserAccountService/UpdateUserDevice` | `iam.user_device.update` |  | Update UserDevice. |
| 145 | `DELETE /v1/iam/users/{userAccountId}/devices/{userDeviceId}` | `UserAccountService/DeleteUserDevice` | `iam.user_device.delete` |  | Delete UserDevice. |
| 146 | `GET /v1/iam/users/{userAccountId}/preferences` | `UserAccountService/ListUserPreferences` | `iam.user_preference.read` |  | List UserPreferences. |
| 147 | `POST /v1/iam/users/{userAccountId}/preferences` | `UserAccountService/CreateUserPreference` | `iam.user_preference.create` |  | Create UserPreference. |
| 148 | `GET /v1/iam/users/{userAccountId}/preferences/{userPreferenceId}` | `UserAccountService/GetUserPreference` | `iam.user_preference.read` |  | Get UserPreference. |
| 149 | `PATCH /v1/iam/users/{userAccountId}/preferences/{userPreferenceId}` | `UserAccountService/UpdateUserPreference` | `iam.user_preference.update` |  | Update UserPreference. |
| 150 | `DELETE /v1/iam/users/{userAccountId}/preferences/{userPreferenceId}` | `UserAccountService/DeleteUserPreference` | `iam.user_preference.delete` |  | Delete UserPreference. |
| 151 | `GET /v1/iam/users/{userAccountId}/sessions` | `UserAccountService/ListUserSessions` | `iam.user_session.read` |  | List UserSessions. |
| 152 | `GET /v1/iam/users/{userAccountId}/sessions/{userSessionId}` | `UserAccountService/GetUserSession` | `iam.user_session.read` |  | Get UserSession. |
| 153 | `DELETE /v1/iam/users/{userAccountId}/sessions/{userSessionId}` | `UserAccountService/DeleteUserSession` | `iam.user_session.delete` |  | Delete UserSession. |
| 154 | `POST /v1/iam/auth:login` | `UserAccountService/Login` | `iam.user_account.login` | Public | Authenticate with credentials and receive tokens. |
| 155 | `POST /v1/iam/auth:refresh` | `UserAccountService/RefreshToken` | `iam.user_account.refresh_token` | Public | Rotate tokens. |
| 156 | `POST /v1/iam/auth:logout` | `UserAccountService/Logout` | `iam.user_account.logout` |  | Revoke the current (or all) sessions and unregister the device. |
| 157 | `POST /v1/iam/auth:signUp` | `UserAccountService/SignUp` | `iam.user_account.sign_up` | Public | Self-registration of a student (when the tenant allows signup). |
| 158 | `POST /v1/iam/auth:requestPasswordReset` | `UserAccountService/RequestPasswordReset` | `iam.user_account.request_password_reset` | Public | Send a reset link / OTP. |
| 159 | `POST /v1/iam/auth:resetPassword` | `UserAccountService/ResetPassword` | `iam.user_account.reset_password` | Public | Complete a password reset. |
| 160 | `POST /v1/iam/auth:changePassword` | `UserAccountService/ChangePassword` | `iam.user_account.change_password` |  | Change the caller's password. |
| 161 | `POST /v1/iam/auth:requestVerification` | `UserAccountService/RequestVerification` | `iam.user_account.request_verification` |  | Send a verification code to the caller's e-mail or mobile. |
| 162 | `POST /v1/iam/auth:confirmVerification` | `UserAccountService/ConfirmVerification` | `iam.user_account.confirm_verification` | Public | Confirm the code and mark e-mail / mobile verified. |
| 163 | `GET /v1/iam/me` | `UserAccountService/GetMe` | `iam.user_account.get_me` |  | Current user, person, role grants, effective permissions and linked student/staff/guardian records. |
| 164 | `POST /v1/iam/users/{userAccountId}:impersonate` | `UserAccountService/StartImpersonation` | `iam.user_account.start_impersonation` |  | Administrator obtains a non-refreshable access token (maximum 30 minutes) acting as this user. |
| 165 | `POST /v1/iam/auth:stopImpersonation` | `UserAccountService/StopImpersonation` | `iam.user_account.stop_impersonation` |  | Return to the administrator's own session. |
| 166 | `POST /v1/iam/users/{userAccountId}:activate` | `UserAccountService/ActivateUser` | `iam.user_account.activate_user` |  | Set status active. |
| 167 | `POST /v1/iam/users/{userAccountId}:deactivate` | `UserAccountService/DeactivateUser` | `iam.user_account.deactivate_user` |  | Set status inactive (login blocked). |
| 168 | `POST /v1/iam/users/{userAccountId}:unlock` | `UserAccountService/UnlockUser` | `iam.user_account.unlock_user` |  | Clear a lock caused by failed logins. |
| 169 | `POST /v1/iam/users/{userAccountId}:forcePasswordReset` | `UserAccountService/ForcePasswordReset` | `iam.user_account.force_password_reset` |  | Require a new password at next login and send a reset link. |
| 170 | `POST /v1/iam/users/{userAccountId}:createApiToken` | `UserAccountService/CreateApiToken` | `iam.user_account.create_api_token` |  | Create a personal API token. |
| 171 | `POST /v1/iam/users/{userAccountId}:setCredentials` | `UserAccountService/SetUserCredentials` | `iam.user_account.set_user_credentials` |  | Administrator sets a user's login name and a temporary password. |
| 172 | `POST /v1/iam/users/{userAccountId}:setRoles` | `UserAccountService/SetUserRoles` | `iam.user_account.set_user_roles` |  | Replace the user's role grants in one call. |
| 173 | `POST /v1/iam/me:switchRole` | `UserAccountService/SwitchRole` | `iam.user_account.switch_role` |  | Switch the active role: issues new tokens whose claims carry the active role (permissions are evaluated for that role only) and remembers it as last_login_role_id. |
| 174 | `POST /v1/iam/me:acceptTerms` | `UserAccountService/AcceptTerms` | `iam.user_account.accept_terms` |  | Record acceptance of the terms and conditions. |
| 175 | `GET /v1/iam/users:checkUsername` | `UserAccountService/CheckUsernameAvailable` | `iam.user_account.check_username_available` | Public | Whether a login name is free in the tenant. |
| 176 | `POST /v1/iam/users:import` | `UserAccountService/ImportUsers` | `iam.user_account.import_users` |  | Bulk-create users with roles from a file. |
| 177 | `POST /v1/iam/mfa:enroll` | `UserAccountService/EnrollMfa` | `iam.mfa_credential.enroll` |  | Enroll a second factor. |
| 178 | `POST /v1/iam/mfa:complete` | `UserAccountService/CompleteMfa` | `iam.user_account.complete_mfa` | Public | Complete the MFA step of sign-in. |
| 179 | `POST /v1/iam/mfa:disable` | `UserAccountService/DisableMfa` | `iam.mfa_credential.disable` |  | Disable a second factor. |
| 180 | `GET /v1/iam/mfa` | `UserAccountService/ListMfaCredentials` | `iam.mfa_credential.read` |  | List the caller's MFA credentials. |
| 181 | `POST /v1/iam/mfa:regenerateRecoveryCodes` | `UserAccountService/RegenerateRecoveryCodes` | `iam.mfa_credential.regenerate_recovery_codes` |  | Regenerate recovery codes. |
| 182 | `GET /v1/iam/operations` | `OperationService/ListOperations` | `iam.operation.read` |  | Long-running operations of this module started by the caller. |
| 183 | `GET /v1/iam/operations/{operationId}` | `OperationService/GetOperation` | `iam.operation.read` |  | Status of a long-running operation started in this module. |
| 184 | `POST /v1/iam/operations/{operationId}:cancel` | `OperationService/CancelOperation` | `iam.operation.cancel_operation` |  | Request cancellation of a running operation. |
| 185 | `GET /v1/iam/setting-definitions` | `SettingDefinitionService/ListSettingDefinitions` | `iam.setting_definition.read` |  | List SettingDefinitions. |
| 186 | `GET /v1/iam/setting-definitions/{settingDefinitionId}` | `SettingDefinitionService/GetSettingDefinition` | `iam.setting_definition.read` |  | Get SettingDefinition. |
| 187 | `GET /v1/iam/setting-values` | `SettingValueService/ListSettingValues` | `iam.setting_value.read` |  | List SettingValues. |
| 188 | `POST /v1/iam/setting-values` | `SettingValueService/CreateSettingValue` | `iam.setting_value.create` |  | Create SettingValue. |
| 189 | `GET /v1/iam/setting-values/{settingValueId}` | `SettingValueService/GetSettingValue` | `iam.setting_value.read` |  | Get SettingValue. |
| 190 | `PATCH /v1/iam/setting-values/{settingValueId}` | `SettingValueService/UpdateSettingValue` | `iam.setting_value.update` |  | Update SettingValue. |
| 191 | `DELETE /v1/iam/setting-values/{settingValueId}` | `SettingValueService/DeleteSettingValue` | `iam.setting_value.delete` |  | Delete SettingValue. |
| 192 | `GET /v1/iam/me/settings` | `UserAccountService/ListMySettings` | `iam.setting.read_mine` |  | The caller's effective settings. |
| 193 | `PUT /v1/iam/me/settings/{settingKey}` | `UserAccountService/SetMySetting` | `iam.setting.update_mine` |  | Set one of the caller's own settings. |
| 194 | `DELETE /v1/iam/me/settings/{settingKey}` | `UserAccountService/ResetMySetting` | `iam.setting.update_mine` |  | Reset one of the caller's own settings. |
| 195 | `GET /v1/iam/tenant-codes/{tenantCode}` | `UserAccountService/ResolveTenantCode` | `iam.tenant_directory.resolve_tenant_code` | Public | Resolve a school's public tenant code (guests name their school with it). |
| 196 | `GET /v1/iam/tenant-codes/{tenantCode}/sign-up-institutions` | `UserAccountService/ListSignUpInstitutions` | `iam.tenant_directory.list_sign_up_institutions` | Public | List the institutions of the school a guest names that accept self sign-up. |

## Organisation & Calendar (`org`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 197 | `GET /v1/org/institutions` | `InstitutionService/ListInstitutions` | `org.institution.read` |  | List Institutions. |
| 198 | `POST /v1/org/institutions` | `InstitutionService/CreateInstitution` | `org.institution.create` |  | Create Institution. |
| 199 | `GET /v1/org/institutions/{institutionId}` | `InstitutionService/GetInstitution` | `org.institution.read` |  | Get Institution. |
| 200 | `PATCH /v1/org/institutions/{institutionId}` | `InstitutionService/UpdateInstitution` | `org.institution.update` |  | Update Institution. |
| 201 | `DELETE /v1/org/institutions/{institutionId}` | `InstitutionService/DeleteInstitution` | `org.institution.delete` |  | Delete Institution. |
| 202 | `GET /v1/org/institutions:active` | `InstitutionService/ListActiveInstitutions` | `org.institution.list_active` |  | List a tenant's active institutions (internal: called by iam with a service token). |
| 203 | `GET /v1/org/institutions/{institutionId}/attributes` | `InstitutionService/ListInstitutionAttributes` | `org.institution_attribute.read` |  | List InstitutionAttributes. |
| 204 | `POST /v1/org/institutions/{institutionId}/attributes` | `InstitutionService/CreateInstitutionAttribute` | `org.institution_attribute.create` |  | Create InstitutionAttribute. |
| 205 | `GET /v1/org/institutions/{institutionId}/attributes/{institutionAttributeId}` | `InstitutionService/GetInstitutionAttribute` | `org.institution_attribute.read` |  | Get InstitutionAttribute. |
| 206 | `PATCH /v1/org/institutions/{institutionId}/attributes/{institutionAttributeId}` | `InstitutionService/UpdateInstitutionAttribute` | `org.institution_attribute.update` |  | Update InstitutionAttribute. |
| 207 | `DELETE /v1/org/institutions/{institutionId}/attributes/{institutionAttributeId}` | `InstitutionService/DeleteInstitutionAttribute` | `org.institution_attribute.delete` |  | Delete InstitutionAttribute. |
| 208 | `GET /v1/org/departments` | `DepartmentService/ListDepartments` | `org.department.read` |  | List Departments. |
| 209 | `POST /v1/org/departments` | `DepartmentService/CreateDepartment` | `org.department.create` |  | Create Department. |
| 210 | `GET /v1/org/departments/{departmentId}` | `DepartmentService/GetDepartment` | `org.department.read` |  | Get Department. |
| 211 | `PATCH /v1/org/departments/{departmentId}` | `DepartmentService/UpdateDepartment` | `org.department.update` |  | Update Department. |
| 212 | `DELETE /v1/org/departments/{departmentId}` | `DepartmentService/DeleteDepartment` | `org.department.delete` |  | Delete Department. |
| 213 | `GET /v1/org/academic-years` | `AcademicYearService/ListAcademicYears` | `org.academic_year.read` |  | List AcademicYears. |
| 214 | `POST /v1/org/academic-years` | `AcademicYearService/CreateAcademicYear` | `org.academic_year.create` |  | Create AcademicYear. |
| 215 | `GET /v1/org/academic-years/{academicYearId}` | `AcademicYearService/GetAcademicYear` | `org.academic_year.read` |  | Get AcademicYear. |
| 216 | `PATCH /v1/org/academic-years/{academicYearId}` | `AcademicYearService/UpdateAcademicYear` | `org.academic_year.update` |  | Update AcademicYear. |
| 217 | `DELETE /v1/org/academic-years/{academicYearId}` | `AcademicYearService/DeleteAcademicYear` | `org.academic_year.delete` |  | Delete AcademicYear. |
| 218 | `GET /v1/org/academic-years/{academicYearId}/terms` | `AcademicYearService/ListAcademicTerms` | `org.academic_term.read` |  | List AcademicTerms. |
| 219 | `POST /v1/org/academic-years/{academicYearId}/terms` | `AcademicYearService/CreateAcademicTerm` | `org.academic_term.create` |  | Create AcademicTerm. |
| 220 | `GET /v1/org/academic-years/{academicYearId}/terms/{academicTermId}` | `AcademicYearService/GetAcademicTerm` | `org.academic_term.read` |  | Get AcademicTerm. |
| 221 | `PATCH /v1/org/academic-years/{academicYearId}/terms/{academicTermId}` | `AcademicYearService/UpdateAcademicTerm` | `org.academic_term.update` |  | Update AcademicTerm. |
| 222 | `DELETE /v1/org/academic-years/{academicYearId}/terms/{academicTermId}` | `AcademicYearService/DeleteAcademicTerm` | `org.academic_term.delete` |  | Delete AcademicTerm. |
| 223 | `GET /v1/org/institution-types` | `InstitutionTypeService/ListInstitutionTypes` | `org.institution_type.read` |  | List InstitutionTypes. |
| 224 | `POST /v1/org/institution-types` | `InstitutionTypeService/CreateInstitutionType` | `org.institution_type.create` |  | Create InstitutionType. |
| 225 | `GET /v1/org/institution-types/{institutionTypeId}` | `InstitutionTypeService/GetInstitutionType` | `org.institution_type.read` |  | Get InstitutionType. |
| 226 | `PATCH /v1/org/institution-types/{institutionTypeId}` | `InstitutionTypeService/UpdateInstitutionType` | `org.institution_type.update` |  | Update InstitutionType. |
| 227 | `DELETE /v1/org/institution-types/{institutionTypeId}` | `InstitutionTypeService/DeleteInstitutionType` | `org.institution_type.delete` |  | Delete InstitutionType. |
| 228 | `GET /v1/org/attribute-definitions` | `AttributeDefinitionService/ListAttributeDefinitions` | `org.attribute_definition.read` |  | List AttributeDefinitions. |
| 229 | `POST /v1/org/attribute-definitions` | `AttributeDefinitionService/CreateAttributeDefinition` | `org.attribute_definition.create` |  | Create AttributeDefinition. |
| 230 | `GET /v1/org/attribute-definitions/{attributeDefinitionId}` | `AttributeDefinitionService/GetAttributeDefinition` | `org.attribute_definition.read` |  | Get AttributeDefinition. |
| 231 | `PATCH /v1/org/attribute-definitions/{attributeDefinitionId}` | `AttributeDefinitionService/UpdateAttributeDefinition` | `org.attribute_definition.update` |  | Update AttributeDefinition. |
| 232 | `DELETE /v1/org/attribute-definitions/{attributeDefinitionId}` | `AttributeDefinitionService/DeleteAttributeDefinition` | `org.attribute_definition.delete` |  | Delete AttributeDefinition. |
| 233 | `GET /v1/org/institution-types/{institutionTypeId}/attributes` | `InstitutionTypeService/ListInstitutionTypeAttributes` | `org.institution_type_attribute.read` |  | List InstitutionTypeAttributes. |
| 234 | `POST /v1/org/institution-types/{institutionTypeId}/attributes` | `InstitutionTypeService/CreateInstitutionTypeAttribute` | `org.institution_type_attribute.create` |  | Create InstitutionTypeAttribute. |
| 235 | `GET /v1/org/institution-types/{institutionTypeId}/attributes/{institutionTypeAttributeId}` | `InstitutionTypeService/GetInstitutionTypeAttribute` | `org.institution_type_attribute.read` |  | Get InstitutionTypeAttribute. |
| 236 | `PATCH /v1/org/institution-types/{institutionTypeId}/attributes/{institutionTypeAttributeId}` | `InstitutionTypeService/UpdateInstitutionTypeAttribute` | `org.institution_type_attribute.update` |  | Update InstitutionTypeAttribute. |
| 237 | `DELETE /v1/org/institution-types/{institutionTypeId}/attributes/{institutionTypeAttributeId}` | `InstitutionTypeService/DeleteInstitutionTypeAttribute` | `org.institution_type_attribute.delete` |  | Delete InstitutionTypeAttribute. |
| 238 | `GET /v1/org/holidays` | `HolidayService/ListHolidays` | `org.holiday.read` |  | List Holidays. |
| 239 | `POST /v1/org/holidays` | `HolidayService/CreateHoliday` | `org.holiday.create` |  | Create Holiday. |
| 240 | `GET /v1/org/holidays/{holidayId}` | `HolidayService/GetHoliday` | `org.holiday.read` |  | Get Holiday. |
| 241 | `PATCH /v1/org/holidays/{holidayId}` | `HolidayService/UpdateHoliday` | `org.holiday.update` |  | Update Holiday. |
| 242 | `DELETE /v1/org/holidays/{holidayId}` | `HolidayService/DeleteHoliday` | `org.holiday.delete` |  | Delete Holiday. |
| 243 | `GET /v1/org/institutions:find` | `InstitutionService/FindInstitutions` | `org.institution.find_institutions` | Public | Find a school / college to sign in to (mobile app school picker). |
| 244 | `POST /v1/org/academic-years/{academicYearId}:activate` | `AcademicYearService/ActivateAcademicYear` | `org.academic_year.activate_academic_year` |  | Make the year active and current for its institution. |
| 245 | `POST /v1/org/academic-years/{academicYearId}:close` | `AcademicYearService/CloseAcademicYear` | `org.academic_year.close_academic_year` |  | Close the year (read-only afterwards). |
| 246 | `POST /v1/org/academic-years/{academicYearId}:generateTerms` | `AcademicYearService/GenerateTerms` | `org.academic_year.generate_terms` |  | Create terms from term_structure. |

## People & Profiles (`people`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 247 | `GET /v1/people/honorifics` | `HonorificService/ListHonorifics` | `people.honorific.read` |  | List Honorifics. |
| 248 | `POST /v1/people/honorifics` | `HonorificService/CreateHonorific` | `people.honorific.create` |  | Create Honorific. |
| 249 | `GET /v1/people/honorifics/{honorificId}` | `HonorificService/GetHonorific` | `people.honorific.read` |  | Get Honorific. |
| 250 | `PATCH /v1/people/honorifics/{honorificId}` | `HonorificService/UpdateHonorific` | `people.honorific.update` |  | Update Honorific. |
| 251 | `DELETE /v1/people/honorifics/{honorificId}` | `HonorificService/DeleteHonorific` | `people.honorific.delete` |  | Delete Honorific. |
| 252 | `GET /v1/people/persons` | `PersonService/ListPersons` | `people.person.read` |  | List Persons. |
| 253 | `POST /v1/people/persons` | `PersonService/CreatePerson` | `people.person.create` |  | Create Person. |
| 254 | `GET /v1/people/persons/{personId}` | `PersonService/GetPerson` | `people.person.read` |  | Get Person. |
| 255 | `PATCH /v1/people/persons/{personId}` | `PersonService/UpdatePerson` | `people.person.update` |  | Update Person. |
| 256 | `DELETE /v1/people/persons/{personId}` | `PersonService/DeletePerson` | `people.person.delete` |  | Delete Person. |
| 257 | `GET /v1/people/persons/{personId}/addresses` | `PersonService/ListPersonAddresses` | `people.person_address.read` |  | List PersonAddresses. |
| 258 | `POST /v1/people/persons/{personId}/addresses` | `PersonService/CreatePersonAddress` | `people.person_address.create` |  | Create PersonAddress. |
| 259 | `GET /v1/people/persons/{personId}/addresses/{personAddressId}` | `PersonService/GetPersonAddress` | `people.person_address.read` |  | Get PersonAddress. |
| 260 | `PATCH /v1/people/persons/{personId}/addresses/{personAddressId}` | `PersonService/UpdatePersonAddress` | `people.person_address.update` |  | Update PersonAddress. |
| 261 | `DELETE /v1/people/persons/{personId}/addresses/{personAddressId}` | `PersonService/DeletePersonAddress` | `people.person_address.delete` |  | Delete PersonAddress. |
| 262 | `GET /v1/people/persons/{personId}/emergency-contacts` | `PersonService/ListEmergencyContacts` | `people.emergency_contact.read` |  | List EmergencyContacts. |
| 263 | `POST /v1/people/persons/{personId}/emergency-contacts` | `PersonService/CreateEmergencyContact` | `people.emergency_contact.create` |  | Create EmergencyContact. |
| 264 | `GET /v1/people/persons/{personId}/emergency-contacts/{emergencyContactId}` | `PersonService/GetEmergencyContact` | `people.emergency_contact.read` |  | Get EmergencyContact. |
| 265 | `PATCH /v1/people/persons/{personId}/emergency-contacts/{emergencyContactId}` | `PersonService/UpdateEmergencyContact` | `people.emergency_contact.update` |  | Update EmergencyContact. |
| 266 | `DELETE /v1/people/persons/{personId}/emergency-contacts/{emergencyContactId}` | `PersonService/DeleteEmergencyContact` | `people.emergency_contact.delete` |  | Delete EmergencyContact. |
| 267 | `GET /v1/people/qualifications` | `QualificationService/ListQualifications` | `people.qualification.read` |  | List Qualifications. |
| 268 | `POST /v1/people/qualifications` | `QualificationService/CreateQualification` | `people.qualification.create` |  | Create Qualification. |
| 269 | `GET /v1/people/qualifications/{qualificationId}` | `QualificationService/GetQualification` | `people.qualification.read` |  | Get Qualification. |
| 270 | `PATCH /v1/people/qualifications/{qualificationId}` | `QualificationService/UpdateQualification` | `people.qualification.update` |  | Update Qualification. |
| 271 | `DELETE /v1/people/qualifications/{qualificationId}` | `QualificationService/DeleteQualification` | `people.qualification.delete` |  | Delete Qualification. |
| 272 | `GET /v1/people/persons/{personId}/education` | `PersonService/ListEducationHistory` | `people.education_history.read` |  | List EducationHistory. |
| 273 | `POST /v1/people/persons/{personId}/education` | `PersonService/CreateEducationHistory` | `people.education_history.create` |  | Create EducationHistory. |
| 274 | `GET /v1/people/persons/{personId}/education/{educationHistoryId}` | `PersonService/GetEducationHistory` | `people.education_history.read` |  | Get EducationHistory. |
| 275 | `PATCH /v1/people/persons/{personId}/education/{educationHistoryId}` | `PersonService/UpdateEducationHistory` | `people.education_history.update` |  | Update EducationHistory. |
| 276 | `DELETE /v1/people/persons/{personId}/education/{educationHistoryId}` | `PersonService/DeleteEducationHistory` | `people.education_history.delete` |  | Delete EducationHistory. |
| 277 | `GET /v1/people/persons/{personId}/work-experience` | `PersonService/ListWorkExperiences` | `people.work_experience.read` |  | List WorkExperiences. |
| 278 | `POST /v1/people/persons/{personId}/work-experience` | `PersonService/CreateWorkExperience` | `people.work_experience.create` |  | Create WorkExperience. |
| 279 | `GET /v1/people/persons/{personId}/work-experience/{workExperienceId}` | `PersonService/GetWorkExperience` | `people.work_experience.read` |  | Get WorkExperience. |
| 280 | `PATCH /v1/people/persons/{personId}/work-experience/{workExperienceId}` | `PersonService/UpdateWorkExperience` | `people.work_experience.update` |  | Update WorkExperience. |
| 281 | `DELETE /v1/people/persons/{personId}/work-experience/{workExperienceId}` | `PersonService/DeleteWorkExperience` | `people.work_experience.delete` |  | Delete WorkExperience. |
| 282 | `GET /v1/people/persons/{personId}/certifications` | `PersonService/ListCertifications` | `people.certification.read` |  | List Certifications. |
| 283 | `POST /v1/people/persons/{personId}/certifications` | `PersonService/CreateCertification` | `people.certification.create` |  | Create Certification. |
| 284 | `GET /v1/people/persons/{personId}/certifications/{certificationId}` | `PersonService/GetCertification` | `people.certification.read` |  | Get Certification. |
| 285 | `PATCH /v1/people/persons/{personId}/certifications/{certificationId}` | `PersonService/UpdateCertification` | `people.certification.update` |  | Update Certification. |
| 286 | `DELETE /v1/people/persons/{personId}/certifications/{certificationId}` | `PersonService/DeleteCertification` | `people.certification.delete` |  | Delete Certification. |
| 287 | `GET /v1/people/skills` | `SkillService/ListSkills` | `people.skill.read` |  | List Skills. |
| 288 | `POST /v1/people/skills` | `SkillService/CreateSkill` | `people.skill.create` |  | Create Skill. |
| 289 | `GET /v1/people/skills/{skillId}` | `SkillService/GetSkill` | `people.skill.read` |  | Get Skill. |
| 290 | `PATCH /v1/people/skills/{skillId}` | `SkillService/UpdateSkill` | `people.skill.update` |  | Update Skill. |
| 291 | `DELETE /v1/people/skills/{skillId}` | `SkillService/DeleteSkill` | `people.skill.delete` |  | Delete Skill. |
| 292 | `GET /v1/people/persons/{personId}/skills` | `PersonService/ListPersonSkills` | `people.person_skill.read` |  | List PersonSkills. |
| 293 | `POST /v1/people/persons/{personId}/skills` | `PersonService/CreatePersonSkill` | `people.person_skill.create` |  | Create PersonSkill. |
| 294 | `GET /v1/people/persons/{personId}/skills/{personSkillId}` | `PersonService/GetPersonSkill` | `people.person_skill.read` |  | Get PersonSkill. |
| 295 | `PATCH /v1/people/persons/{personId}/skills/{personSkillId}` | `PersonService/UpdatePersonSkill` | `people.person_skill.update` |  | Update PersonSkill. |
| 296 | `DELETE /v1/people/persons/{personId}/skills/{personSkillId}` | `PersonService/DeletePersonSkill` | `people.person_skill.delete` |  | Delete PersonSkill. |
| 297 | `GET /v1/people/persons/{personId}/documents` | `PersonService/ListPersonDocuments` | `people.person_document.read` |  | List PersonDocuments. |
| 298 | `POST /v1/people/persons/{personId}/documents` | `PersonService/CreatePersonDocument` | `people.person_document.create` |  | Create PersonDocument. |
| 299 | `GET /v1/people/persons/{personId}/documents/{personDocumentId}` | `PersonService/GetPersonDocument` | `people.person_document.read` |  | Get PersonDocument. |
| 300 | `PATCH /v1/people/persons/{personId}/documents/{personDocumentId}` | `PersonService/UpdatePersonDocument` | `people.person_document.update` |  | Update PersonDocument. |
| 301 | `DELETE /v1/people/persons/{personId}/documents/{personDocumentId}` | `PersonService/DeletePersonDocument` | `people.person_document.delete` |  | Delete PersonDocument. |
| 302 | `POST /v1/people/persons/{personId}/public-profile` | `PersonService/CreatePublicProfile` | `people.public_profile.create` |  | Create PublicProfile. |
| 303 | `GET /v1/people/persons/{personId}/public-profile/{publicProfileId}` | `PersonService/GetPublicProfile` | `people.public_profile.read` |  | Get PublicProfile. |
| 304 | `PATCH /v1/people/persons/{personId}/public-profile/{publicProfileId}` | `PersonService/UpdatePublicProfile` | `people.public_profile.update` |  | Update PublicProfile. |
| 305 | `DELETE /v1/people/persons/{personId}/public-profile/{publicProfileId}` | `PersonService/DeletePublicProfile` | `people.public_profile.delete` |  | Delete PublicProfile. |
| 306 | `GET /v1/people/persons/{personId}/social-links` | `PersonService/ListPersonSocialLinks` | `people.person_social_link.read` |  | List PersonSocialLinks. |
| 307 | `POST /v1/people/persons/{personId}/social-links` | `PersonService/CreatePersonSocialLink` | `people.person_social_link.create` |  | Create PersonSocialLink. |
| 308 | `GET /v1/people/persons/{personId}/social-links/{personSocialLinkId}` | `PersonService/GetPersonSocialLink` | `people.person_social_link.read` |  | Get PersonSocialLink. |
| 309 | `PATCH /v1/people/persons/{personId}/social-links/{personSocialLinkId}` | `PersonService/UpdatePersonSocialLink` | `people.person_social_link.update` |  | Update PersonSocialLink. |
| 310 | `DELETE /v1/people/persons/{personId}/social-links/{personSocialLinkId}` | `PersonService/DeletePersonSocialLink` | `people.person_social_link.delete` |  | Delete PersonSocialLink. |
| 311 | `GET /v1/people/persons/{personId}/consents` | `PersonService/ListConsentRecords` | `people.consent_record.read` |  | List ConsentRecords. |
| 312 | `POST /v1/people/persons/{personId}/consents` | `PersonService/CreateConsentRecord` | `people.consent_record.create` |  | Create ConsentRecord. |
| 313 | `GET /v1/people/persons/{personId}/consents/{consentRecordId}` | `PersonService/GetConsentRecord` | `people.consent_record.read` |  | Get ConsentRecord. |
| 314 | `GET /v1/people/persons:birthdays` | `PersonService/ListBirthdays` | `people.person.list_birthdays` |  | Birthdays and work anniversaries in a date window. |
| 315 | `POST /v1/people/persons:sendGreetings` | `PersonService/SendGreetings` | `people.person.send_greetings` |  | Send birthday / anniversary greetings. |
| 316 | `POST /v1/people/persons/{personId}/consents/{consentRecordId}:withdraw` | `PersonService/WithdrawConsent` | `people.consent_record.withdraw_consent` |  | Withdraw a consent (sets withdrawnAt). |
| 317 | `GET /v1/people/persons/{personId}:consentStatus` | `PersonService/GetConsentStatus` | `people.person.get_consent_status` |  | Consents currently in force for a person, one per purpose (latest record with withdrawnAt empty). |
| 318 | `GET /v1/people/me` | `PersonService/GetMyPerson` | `people.person.read_mine` |  | The caller's own person record. |
| 319 | `PATCH /v1/people/me` | `PersonService/UpdateMyPerson` | `people.person.update_mine` |  | Update the caller's own person record. |

## Academic Structure (`academics`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 320 | `GET /v1/academics/program-levels` | `ProgramLevelService/ListProgramLevels` | `academics.program_level.read` |  | List ProgramLevels. |
| 321 | `POST /v1/academics/program-levels` | `ProgramLevelService/CreateProgramLevel` | `academics.program_level.create` |  | Create ProgramLevel. |
| 322 | `GET /v1/academics/program-levels/{programLevelId}` | `ProgramLevelService/GetProgramLevel` | `academics.program_level.read` |  | Get ProgramLevel. |
| 323 | `PATCH /v1/academics/program-levels/{programLevelId}` | `ProgramLevelService/UpdateProgramLevel` | `academics.program_level.update` |  | Update ProgramLevel. |
| 324 | `DELETE /v1/academics/program-levels/{programLevelId}` | `ProgramLevelService/DeleteProgramLevel` | `academics.program_level.delete` |  | Delete ProgramLevel. |
| 325 | `GET /v1/academics/programs` | `ProgramService/ListPrograms` | `academics.program.read` |  | List Programs. |
| 326 | `POST /v1/academics/programs` | `ProgramService/CreateProgram` | `academics.program.create` |  | Create Program. |
| 327 | `GET /v1/academics/programs/{programId}` | `ProgramService/GetProgram` | `academics.program.read` |  | Get Program. |
| 328 | `PATCH /v1/academics/programs/{programId}` | `ProgramService/UpdateProgram` | `academics.program.update` |  | Update Program. |
| 329 | `DELETE /v1/academics/programs/{programId}` | `ProgramService/DeleteProgram` | `academics.program.delete` |  | Delete Program. |
| 330 | `GET /v1/academics/courses` | `CourseService/ListCourses` | `academics.course.read` |  | List Courses. |
| 331 | `POST /v1/academics/courses` | `CourseService/CreateCourse` | `academics.course.create` |  | Create Course. |
| 332 | `GET /v1/academics/courses/{courseId}` | `CourseService/GetCourse` | `academics.course.read` |  | Get Course. |
| 333 | `PATCH /v1/academics/courses/{courseId}` | `CourseService/UpdateCourse` | `academics.course.update` |  | Update Course. |
| 334 | `DELETE /v1/academics/courses/{courseId}` | `CourseService/DeleteCourse` | `academics.course.delete` |  | Delete Course. |
| 335 | `GET /v1/academics/batches` | `BatchService/ListBatches` | `academics.batch.read` |  | List Batches. |
| 336 | `POST /v1/academics/batches` | `BatchService/CreateBatch` | `academics.batch.create` |  | Create Batch. |
| 337 | `GET /v1/academics/batches/{batchId}` | `BatchService/GetBatch` | `academics.batch.read` |  | Get Batch. |
| 338 | `PATCH /v1/academics/batches/{batchId}` | `BatchService/UpdateBatch` | `academics.batch.update` |  | Update Batch. |
| 339 | `DELETE /v1/academics/batches/{batchId}` | `BatchService/DeleteBatch` | `academics.batch.delete` |  | Delete Batch. |
| 340 | `GET /v1/academics/grade-levels` | `GradeLevelService/ListGradeLevels` | `academics.grade_level.read` |  | List GradeLevels. |
| 341 | `POST /v1/academics/grade-levels` | `GradeLevelService/CreateGradeLevel` | `academics.grade_level.create` |  | Create GradeLevel. |
| 342 | `GET /v1/academics/grade-levels/{gradeLevelId}` | `GradeLevelService/GetGradeLevel` | `academics.grade_level.read` |  | Get GradeLevel. |
| 343 | `PATCH /v1/academics/grade-levels/{gradeLevelId}` | `GradeLevelService/UpdateGradeLevel` | `academics.grade_level.update` |  | Update GradeLevel. |
| 344 | `DELETE /v1/academics/grade-levels/{gradeLevelId}` | `GradeLevelService/DeleteGradeLevel` | `academics.grade_level.delete` |  | Delete GradeLevel. |
| 345 | `GET /v1/academics/sections` | `SectionService/ListSections` | `academics.section.read` |  | List Sections. |
| 346 | `POST /v1/academics/sections` | `SectionService/CreateSection` | `academics.section.create` |  | Create Section. |
| 347 | `GET /v1/academics/sections/{sectionId}` | `SectionService/GetSection` | `academics.section.read` |  | Get Section. |
| 348 | `PATCH /v1/academics/sections/{sectionId}` | `SectionService/UpdateSection` | `academics.section.update` |  | Update Section. |
| 349 | `DELETE /v1/academics/sections/{sectionId}` | `SectionService/DeleteSection` | `academics.section.delete` |  | Delete Section. |
| 350 | `GET /v1/academics/class-sections` | `ClassSectionService/ListClassSections` | `academics.class_section.read` |  | List ClassSections. |
| 351 | `POST /v1/academics/class-sections` | `ClassSectionService/CreateClassSection` | `academics.class_section.create` |  | Create ClassSection. |
| 352 | `GET /v1/academics/class-sections/{classSectionId}` | `ClassSectionService/GetClassSection` | `academics.class_section.read` |  | Get ClassSection. |
| 353 | `PATCH /v1/academics/class-sections/{classSectionId}` | `ClassSectionService/UpdateClassSection` | `academics.class_section.update` |  | Update ClassSection. |
| 354 | `DELETE /v1/academics/class-sections/{classSectionId}` | `ClassSectionService/DeleteClassSection` | `academics.class_section.delete` |  | Delete ClassSection. |
| 355 | `GET /v1/academics/subjects` | `SubjectService/ListSubjects` | `academics.subject.read` |  | List Subjects. |
| 356 | `POST /v1/academics/subjects` | `SubjectService/CreateSubject` | `academics.subject.create` |  | Create Subject. |
| 357 | `GET /v1/academics/subjects/{subjectId}` | `SubjectService/GetSubject` | `academics.subject.read` |  | Get Subject. |
| 358 | `PATCH /v1/academics/subjects/{subjectId}` | `SubjectService/UpdateSubject` | `academics.subject.update` |  | Update Subject. |
| 359 | `DELETE /v1/academics/subjects/{subjectId}` | `SubjectService/DeleteSubject` | `academics.subject.delete` |  | Delete Subject. |
| 360 | `GET /v1/academics/courses/{courseId}/subjects` | `CourseService/ListCourseSubjects` | `academics.course_subject.read` |  | List CourseSubjects. |
| 361 | `POST /v1/academics/courses/{courseId}/subjects` | `CourseService/CreateCourseSubject` | `academics.course_subject.create` |  | Create CourseSubject. |
| 362 | `GET /v1/academics/courses/{courseId}/subjects/{courseSubjectId}` | `CourseService/GetCourseSubject` | `academics.course_subject.read` |  | Get CourseSubject. |
| 363 | `PATCH /v1/academics/courses/{courseId}/subjects/{courseSubjectId}` | `CourseService/UpdateCourseSubject` | `academics.course_subject.update` |  | Update CourseSubject. |
| 364 | `DELETE /v1/academics/courses/{courseId}/subjects/{courseSubjectId}` | `CourseService/DeleteCourseSubject` | `academics.course_subject.delete` |  | Delete CourseSubject. |
| 365 | `GET /v1/academics/class-sections/{classSectionId}/subjects` | `ClassSectionService/ListClassSubjects` | `academics.class_subject.read` |  | List ClassSubjects. |
| 366 | `POST /v1/academics/class-sections/{classSectionId}/subjects` | `ClassSectionService/CreateClassSubject` | `academics.class_subject.create` |  | Create ClassSubject. |
| 367 | `GET /v1/academics/class-sections/{classSectionId}/subjects/{classSubjectId}` | `ClassSectionService/GetClassSubject` | `academics.class_subject.read` |  | Get ClassSubject. |
| 368 | `PATCH /v1/academics/class-sections/{classSectionId}/subjects/{classSubjectId}` | `ClassSectionService/UpdateClassSubject` | `academics.class_subject.update` |  | Update ClassSubject. |
| 369 | `DELETE /v1/academics/class-sections/{classSectionId}/subjects/{classSubjectId}` | `ClassSectionService/DeleteClassSubject` | `academics.class_subject.delete` |  | Delete ClassSubject. |
| 370 | `GET /v1/academics/teaching-assignments` | `TeachingAssignmentService/ListTeachingAssignments` | `academics.teaching_assignment.read` |  | List TeachingAssignments. |
| 371 | `POST /v1/academics/teaching-assignments` | `TeachingAssignmentService/CreateTeachingAssignment` | `academics.teaching_assignment.create` |  | Create TeachingAssignment. |
| 372 | `GET /v1/academics/teaching-assignments/{teachingAssignmentId}` | `TeachingAssignmentService/GetTeachingAssignment` | `academics.teaching_assignment.read` |  | Get TeachingAssignment. |
| 373 | `PATCH /v1/academics/teaching-assignments/{teachingAssignmentId}` | `TeachingAssignmentService/UpdateTeachingAssignment` | `academics.teaching_assignment.update` |  | Update TeachingAssignment. |
| 374 | `DELETE /v1/academics/teaching-assignments/{teachingAssignmentId}` | `TeachingAssignmentService/DeleteTeachingAssignment` | `academics.teaching_assignment.delete` |  | Delete TeachingAssignment. |
| 375 | `GET /v1/academics/curriculum-units` | `CurriculumUnitService/ListCurriculumUnits` | `academics.curriculum_unit.read` |  | List CurriculumUnits. |
| 376 | `POST /v1/academics/curriculum-units` | `CurriculumUnitService/CreateCurriculumUnit` | `academics.curriculum_unit.create` |  | Create CurriculumUnit. |
| 377 | `GET /v1/academics/curriculum-units/{curriculumUnitId}` | `CurriculumUnitService/GetCurriculumUnit` | `academics.curriculum_unit.read` |  | Get CurriculumUnit. |
| 378 | `PATCH /v1/academics/curriculum-units/{curriculumUnitId}` | `CurriculumUnitService/UpdateCurriculumUnit` | `academics.curriculum_unit.update` |  | Update CurriculumUnit. |
| 379 | `DELETE /v1/academics/curriculum-units/{curriculumUnitId}` | `CurriculumUnitService/DeleteCurriculumUnit` | `academics.curriculum_unit.delete` |  | Delete CurriculumUnit. |
| 380 | `GET /v1/academics/lesson-plans` | `LessonPlanService/ListLessonPlans` | `academics.lesson_plan.read` |  | List LessonPlans. |
| 381 | `POST /v1/academics/lesson-plans` | `LessonPlanService/CreateLessonPlan` | `academics.lesson_plan.create` |  | Create LessonPlan. |
| 382 | `GET /v1/academics/lesson-plans/{lessonPlanId}` | `LessonPlanService/GetLessonPlan` | `academics.lesson_plan.read` |  | Get LessonPlan. |
| 383 | `PATCH /v1/academics/lesson-plans/{lessonPlanId}` | `LessonPlanService/UpdateLessonPlan` | `academics.lesson_plan.update` |  | Update LessonPlan. |
| 384 | `DELETE /v1/academics/lesson-plans/{lessonPlanId}` | `LessonPlanService/DeleteLessonPlan` | `academics.lesson_plan.delete` |  | Delete LessonPlan. |
| 385 | `GET /v1/academics/lesson-plans/{lessonPlanId}/approvals` | `LessonPlanService/ListLessonPlanApprovals` | `academics.lesson_plan_approval.read` |  | List LessonPlanApprovals. |
| 386 | `POST /v1/academics/lesson-plans/{lessonPlanId}/approvals` | `LessonPlanService/CreateLessonPlanApproval` | `academics.lesson_plan_approval.create` |  | Create LessonPlanApproval. |
| 387 | `GET /v1/academics/lesson-plans/{lessonPlanId}/approvals/{lessonPlanApprovalId}` | `LessonPlanService/GetLessonPlanApproval` | `academics.lesson_plan_approval.read` |  | Get LessonPlanApproval. |
| 388 | `PATCH /v1/academics/lesson-plans/{lessonPlanId}/approvals/{lessonPlanApprovalId}` | `LessonPlanService/UpdateLessonPlanApproval` | `academics.lesson_plan_approval.update` |  | Update LessonPlanApproval. |
| 389 | `DELETE /v1/academics/lesson-plans/{lessonPlanId}/approvals/{lessonPlanApprovalId}` | `LessonPlanService/DeleteLessonPlanApproval` | `academics.lesson_plan_approval.delete` |  | Delete LessonPlanApproval. |
| 390 | `POST /v1/academics/lesson-plans/{lessonPlanId}:submit` | `LessonPlanService/SubmitLessonPlan` | `academics.lesson_plan.submit_lesson_plan` |  | Submit for approval. |
| 391 | `POST /v1/academics/lesson-plans/{lessonPlanId}:approve` | `LessonPlanService/ApproveLessonPlan` | `academics.lesson_plan.approve_lesson_plan` |  | Approve (records an approval). |
| 392 | `POST /v1/academics/lesson-plans/{lessonPlanId}:reject` | `LessonPlanService/RejectLessonPlan` | `academics.lesson_plan.reject_lesson_plan` |  | Reject. |
| 393 | `POST /v1/academics/lesson-plans/{lessonPlanId}:publish` | `LessonPlanService/PublishLessonPlan` | `academics.lesson_plan.publish_lesson_plan` |  | Publish to students / guardians. |
| 394 | `POST /v1/academics/lesson-plans/{lessonPlanId}:archive` | `LessonPlanService/ArchiveLessonPlan` | `academics.lesson_plan.archive_lesson_plan` |  | Archive. |

## Student Information (`sis`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 395 | `GET /v1/sis/student-categories` | `StudentCategoryService/ListStudentCategories` | `sis.student_category.read` |  | List StudentCategories. |
| 396 | `POST /v1/sis/student-categories` | `StudentCategoryService/CreateStudentCategory` | `sis.student_category.create` |  | Create StudentCategory. |
| 397 | `GET /v1/sis/student-categories/{studentCategoryId}` | `StudentCategoryService/GetStudentCategory` | `sis.student_category.read` |  | Get StudentCategory. |
| 398 | `PATCH /v1/sis/student-categories/{studentCategoryId}` | `StudentCategoryService/UpdateStudentCategory` | `sis.student_category.update` |  | Update StudentCategory. |
| 399 | `DELETE /v1/sis/student-categories/{studentCategoryId}` | `StudentCategoryService/DeleteStudentCategory` | `sis.student_category.delete` |  | Delete StudentCategory. |
| 400 | `GET /v1/sis/students` | `StudentService/ListStudents` | `sis.student.read` |  | List Students. |
| 401 | `POST /v1/sis/students` | `StudentService/CreateStudent` | `sis.student.create` |  | Create Student. |
| 402 | `GET /v1/sis/students/{studentId}` | `StudentService/GetStudent` | `sis.student.read` |  | Get Student. |
| 403 | `PATCH /v1/sis/students/{studentId}` | `StudentService/UpdateStudent` | `sis.student.update` |  | Update Student. |
| 404 | `DELETE /v1/sis/students/{studentId}` | `StudentService/DeleteStudent` | `sis.student.delete` |  | Delete Student. |
| 405 | `GET /v1/sis/guardians` | `GuardianService/ListGuardians` | `sis.guardian.read` |  | List Guardians. |
| 406 | `POST /v1/sis/guardians` | `GuardianService/CreateGuardian` | `sis.guardian.create` |  | Create Guardian. |
| 407 | `GET /v1/sis/guardians/{guardianId}` | `GuardianService/GetGuardian` | `sis.guardian.read` |  | Get Guardian. |
| 408 | `PATCH /v1/sis/guardians/{guardianId}` | `GuardianService/UpdateGuardian` | `sis.guardian.update` |  | Update Guardian. |
| 409 | `DELETE /v1/sis/guardians/{guardianId}` | `GuardianService/DeleteGuardian` | `sis.guardian.delete` |  | Delete Guardian. |
| 410 | `GET /v1/sis/relationship-types` | `RelationshipTypeService/ListRelationshipTypes` | `sis.relationship_type.read` |  | List RelationshipTypes. |
| 411 | `POST /v1/sis/relationship-types` | `RelationshipTypeService/CreateRelationshipType` | `sis.relationship_type.create` |  | Create RelationshipType. |
| 412 | `GET /v1/sis/relationship-types/{relationshipTypeId}` | `RelationshipTypeService/GetRelationshipType` | `sis.relationship_type.read` |  | Get RelationshipType. |
| 413 | `PATCH /v1/sis/relationship-types/{relationshipTypeId}` | `RelationshipTypeService/UpdateRelationshipType` | `sis.relationship_type.update` |  | Update RelationshipType. |
| 414 | `DELETE /v1/sis/relationship-types/{relationshipTypeId}` | `RelationshipTypeService/DeleteRelationshipType` | `sis.relationship_type.delete` |  | Delete RelationshipType. |
| 415 | `GET /v1/sis/students/{studentId}/guardians` | `StudentService/ListStudentGuardians` | `sis.student_guardian.read` |  | List StudentGuardians. |
| 416 | `POST /v1/sis/students/{studentId}/guardians` | `StudentService/CreateStudentGuardian` | `sis.student_guardian.create` |  | Create StudentGuardian. |
| 417 | `GET /v1/sis/students/{studentId}/guardians/{studentGuardianId}` | `StudentService/GetStudentGuardian` | `sis.student_guardian.read` |  | Get StudentGuardian. |
| 418 | `PATCH /v1/sis/students/{studentId}/guardians/{studentGuardianId}` | `StudentService/UpdateStudentGuardian` | `sis.student_guardian.update` |  | Update StudentGuardian. |
| 419 | `DELETE /v1/sis/students/{studentId}/guardians/{studentGuardianId}` | `StudentService/DeleteStudentGuardian` | `sis.student_guardian.delete` |  | Delete StudentGuardian. |
| 420 | `GET /v1/sis/students/{studentId}/siblings` | `StudentService/ListStudentSiblings` | `sis.student_sibling.read` |  | List StudentSiblings. |
| 421 | `POST /v1/sis/students/{studentId}/siblings` | `StudentService/CreateStudentSibling` | `sis.student_sibling.create` |  | Create StudentSibling. |
| 422 | `GET /v1/sis/students/{studentId}/siblings/{studentSiblingId}` | `StudentService/GetStudentSibling` | `sis.student_sibling.read` |  | Get StudentSibling. |
| 423 | `PATCH /v1/sis/students/{studentId}/siblings/{studentSiblingId}` | `StudentService/UpdateStudentSibling` | `sis.student_sibling.update` |  | Update StudentSibling. |
| 424 | `DELETE /v1/sis/students/{studentId}/siblings/{studentSiblingId}` | `StudentService/DeleteStudentSibling` | `sis.student_sibling.delete` |  | Delete StudentSibling. |
| 425 | `GET /v1/sis/student-enrollments` | `StudentEnrollmentService/ListStudentEnrollments` | `sis.student_enrollment.read` |  | List StudentEnrollments. |
| 426 | `POST /v1/sis/student-enrollments` | `StudentEnrollmentService/CreateStudentEnrollment` | `sis.student_enrollment.create` |  | Create StudentEnrollment. |
| 427 | `GET /v1/sis/student-enrollments/{studentEnrollmentId}` | `StudentEnrollmentService/GetStudentEnrollment` | `sis.student_enrollment.read` |  | Get StudentEnrollment. |
| 428 | `PATCH /v1/sis/student-enrollments/{studentEnrollmentId}` | `StudentEnrollmentService/UpdateStudentEnrollment` | `sis.student_enrollment.update` |  | Update StudentEnrollment. |
| 429 | `DELETE /v1/sis/student-enrollments/{studentEnrollmentId}` | `StudentEnrollmentService/DeleteStudentEnrollment` | `sis.student_enrollment.delete` |  | Delete StudentEnrollment. |
| 430 | `GET /v1/sis/program-enrollments` | `ProgramEnrollmentService/ListProgramEnrollments` | `sis.program_enrollment.read` |  | List ProgramEnrollments. |
| 431 | `POST /v1/sis/program-enrollments` | `ProgramEnrollmentService/CreateProgramEnrollment` | `sis.program_enrollment.create` |  | Create ProgramEnrollment. |
| 432 | `GET /v1/sis/program-enrollments/{programEnrollmentId}` | `ProgramEnrollmentService/GetProgramEnrollment` | `sis.program_enrollment.read` |  | Get ProgramEnrollment. |
| 433 | `PATCH /v1/sis/program-enrollments/{programEnrollmentId}` | `ProgramEnrollmentService/UpdateProgramEnrollment` | `sis.program_enrollment.update` |  | Update ProgramEnrollment. |
| 434 | `DELETE /v1/sis/program-enrollments/{programEnrollmentId}` | `ProgramEnrollmentService/DeleteProgramEnrollment` | `sis.program_enrollment.delete` |  | Delete ProgramEnrollment. |
| 435 | `GET /v1/sis/program-enrollments/{programEnrollmentId}/subjects` | `ProgramEnrollmentService/ListProgramEnrollmentSubjects` | `sis.program_enrollment_subject.read` |  | List ProgramEnrollmentSubjects. |
| 436 | `POST /v1/sis/program-enrollments/{programEnrollmentId}/subjects` | `ProgramEnrollmentService/CreateProgramEnrollmentSubject` | `sis.program_enrollment_subject.create` |  | Create ProgramEnrollmentSubject. |
| 437 | `GET /v1/sis/program-enrollments/{programEnrollmentId}/subjects/{programEnrollmentSubjectId}` | `ProgramEnrollmentService/GetProgramEnrollmentSubject` | `sis.program_enrollment_subject.read` |  | Get ProgramEnrollmentSubject. |
| 438 | `PATCH /v1/sis/program-enrollments/{programEnrollmentId}/subjects/{programEnrollmentSubjectId}` | `ProgramEnrollmentService/UpdateProgramEnrollmentSubject` | `sis.program_enrollment_subject.update` |  | Update ProgramEnrollmentSubject. |
| 439 | `DELETE /v1/sis/program-enrollments/{programEnrollmentId}/subjects/{programEnrollmentSubjectId}` | `ProgramEnrollmentService/DeleteProgramEnrollmentSubject` | `sis.program_enrollment_subject.delete` |  | Delete ProgramEnrollmentSubject. |
| 440 | `GET /v1/sis/subject-registrations` | `SubjectRegistrationService/ListSubjectRegistrations` | `sis.subject_registration.read` |  | List SubjectRegistrations. |
| 441 | `POST /v1/sis/subject-registrations` | `SubjectRegistrationService/CreateSubjectRegistration` | `sis.subject_registration.create` |  | Create SubjectRegistration. |
| 442 | `GET /v1/sis/subject-registrations/{subjectRegistrationId}` | `SubjectRegistrationService/GetSubjectRegistration` | `sis.subject_registration.read` |  | Get SubjectRegistration. |
| 443 | `PATCH /v1/sis/subject-registrations/{subjectRegistrationId}` | `SubjectRegistrationService/UpdateSubjectRegistration` | `sis.subject_registration.update` |  | Update SubjectRegistration. |
| 444 | `DELETE /v1/sis/subject-registrations/{subjectRegistrationId}` | `SubjectRegistrationService/DeleteSubjectRegistration` | `sis.subject_registration.delete` |  | Delete SubjectRegistration. |
| 445 | `GET /v1/sis/subject-registrations/{subjectRegistrationId}/lines` | `SubjectRegistrationService/ListSubjectRegistrationLines` | `sis.subject_registration_line.read` |  | List SubjectRegistrationLines. |
| 446 | `POST /v1/sis/subject-registrations/{subjectRegistrationId}/lines` | `SubjectRegistrationService/CreateSubjectRegistrationLine` | `sis.subject_registration_line.create` |  | Create SubjectRegistrationLine. |
| 447 | `GET /v1/sis/subject-registrations/{subjectRegistrationId}/lines/{subjectRegistrationLineId}` | `SubjectRegistrationService/GetSubjectRegistrationLine` | `sis.subject_registration_line.read` |  | Get SubjectRegistrationLine. |
| 448 | `PATCH /v1/sis/subject-registrations/{subjectRegistrationId}/lines/{subjectRegistrationLineId}` | `SubjectRegistrationService/UpdateSubjectRegistrationLine` | `sis.subject_registration_line.update` |  | Update SubjectRegistrationLine. |
| 449 | `DELETE /v1/sis/subject-registrations/{subjectRegistrationId}/lines/{subjectRegistrationLineId}` | `SubjectRegistrationService/DeleteSubjectRegistrationLine` | `sis.subject_registration_line.delete` |  | Delete SubjectRegistrationLine. |
| 450 | `GET /v1/sis/promotions` | `PromotionService/ListPromotions` | `sis.promotion.read` |  | List Promotions. |
| 451 | `POST /v1/sis/promotions` | `PromotionService/CreatePromotion` | `sis.promotion.create` |  | Create Promotion. |
| 452 | `GET /v1/sis/promotions/{promotionId}` | `PromotionService/GetPromotion` | `sis.promotion.read` |  | Get Promotion. |
| 453 | `PATCH /v1/sis/promotions/{promotionId}` | `PromotionService/UpdatePromotion` | `sis.promotion.update` |  | Update Promotion. |
| 454 | `DELETE /v1/sis/promotions/{promotionId}` | `PromotionService/DeletePromotion` | `sis.promotion.delete` |  | Delete Promotion. |
| 455 | `GET /v1/sis/student-health-records` | `StudentHealthRecordService/ListStudentHealthRecords` | `sis.student_health_record.read` |  | List StudentHealthRecords. |
| 456 | `POST /v1/sis/student-health-records` | `StudentHealthRecordService/CreateStudentHealthRecord` | `sis.student_health_record.create` |  | Create StudentHealthRecord. |
| 457 | `GET /v1/sis/student-health-records/{studentHealthRecordId}` | `StudentHealthRecordService/GetStudentHealthRecord` | `sis.student_health_record.read` |  | Get StudentHealthRecord. |
| 458 | `PATCH /v1/sis/student-health-records/{studentHealthRecordId}` | `StudentHealthRecordService/UpdateStudentHealthRecord` | `sis.student_health_record.update` |  | Update StudentHealthRecord. |
| 459 | `DELETE /v1/sis/student-health-records/{studentHealthRecordId}` | `StudentHealthRecordService/DeleteStudentHealthRecord` | `sis.student_health_record.delete` |  | Delete StudentHealthRecord. |
| 460 | `GET /v1/sis/discipline-incidents` | `DisciplineIncidentService/ListDisciplineIncidents` | `sis.discipline_incident.read` |  | List DisciplineIncidents. |
| 461 | `POST /v1/sis/discipline-incidents` | `DisciplineIncidentService/CreateDisciplineIncident` | `sis.discipline_incident.create` |  | Create DisciplineIncident. |
| 462 | `GET /v1/sis/discipline-incidents/{disciplineIncidentId}` | `DisciplineIncidentService/GetDisciplineIncident` | `sis.discipline_incident.read` |  | Get DisciplineIncident. |
| 463 | `PATCH /v1/sis/discipline-incidents/{disciplineIncidentId}` | `DisciplineIncidentService/UpdateDisciplineIncident` | `sis.discipline_incident.update` |  | Update DisciplineIncident. |
| 464 | `DELETE /v1/sis/discipline-incidents/{disciplineIncidentId}` | `DisciplineIncidentService/DeleteDisciplineIncident` | `sis.discipline_incident.delete` |  | Delete DisciplineIncident. |
| 465 | `GET /v1/sis/activity-types` | `ActivityTypeService/ListActivityTypes` | `sis.activity_type.read` |  | List ActivityTypes. |
| 466 | `POST /v1/sis/activity-types` | `ActivityTypeService/CreateActivityType` | `sis.activity_type.create` |  | Create ActivityType. |
| 467 | `GET /v1/sis/activity-types/{activityTypeId}` | `ActivityTypeService/GetActivityType` | `sis.activity_type.read` |  | Get ActivityType. |
| 468 | `PATCH /v1/sis/activity-types/{activityTypeId}` | `ActivityTypeService/UpdateActivityType` | `sis.activity_type.update` |  | Update ActivityType. |
| 469 | `DELETE /v1/sis/activity-types/{activityTypeId}` | `ActivityTypeService/DeleteActivityType` | `sis.activity_type.delete` |  | Delete ActivityType. |
| 470 | `GET /v1/sis/student-activities` | `StudentActivityService/ListStudentActivities` | `sis.student_activity.read` |  | List StudentActivities. |
| 471 | `POST /v1/sis/student-activities` | `StudentActivityService/CreateStudentActivity` | `sis.student_activity.create` |  | Create StudentActivity. |
| 472 | `GET /v1/sis/student-activities/{studentActivityId}` | `StudentActivityService/GetStudentActivity` | `sis.student_activity.read` |  | Get StudentActivity. |
| 473 | `PATCH /v1/sis/student-activities/{studentActivityId}` | `StudentActivityService/UpdateStudentActivity` | `sis.student_activity.update` |  | Update StudentActivity. |
| 474 | `DELETE /v1/sis/student-activities/{studentActivityId}` | `StudentActivityService/DeleteStudentActivity` | `sis.student_activity.delete` |  | Delete StudentActivity. |
| 475 | `POST /v1/sis/students/{studentId}:changeStatus` | `StudentService/ChangeStudentStatus` | `sis.student.change_student_status` |  | Change status (active, inactive, withdrawn, transferred, alumni, graduated) with exit details. |
| 476 | `GET /v1/sis/students/{studentId}:summary` | `StudentService/GetStudentSummary` | `sis.student.get_student_summary` |  | Student 360 overview. |
| 477 | `POST /v1/sis/students/{studentId}:issueBonafideCertificate` | `StudentService/IssueBonafideCertificate` | `sis.student.issue_bonafide_certificate` |  | Generate a bonafide certificate PDF and record its number. |
| 478 | `POST /v1/sis/students/{studentId}:createPortalAccount` | `StudentService/CreateStudentPortalAccount` | `sis.student.create_student_portal_account` |  | Create the student login account and grant the student role. |
| 479 | `POST /v1/sis/guardians/{guardianId}:createPortalAccount` | `GuardianService/CreatePortalAccount` | `sis.guardian.create_portal_account` |  | Create the guardian's login account and grant the guardian role scoped to their children. |
| 480 | `GET /v1/sis/me/children` | `GuardianService/ListMyChildren` | `sis.guardian.list_my_children` |  | Children of the calling guardian. |
| 481 | `POST /v1/sis/promotions:propose` | `PromotionService/ProposePromotions` | `sis.promotion.propose_promotions` |  | Create proposed promotions for a class. |
| 482 | `POST /v1/sis/promotions/{promotionId}:approve` | `PromotionService/ApprovePromotion` | `sis.promotion.approve_promotion` |  | Approve. |
| 483 | `POST /v1/sis/promotions/{promotionId}:reject` | `PromotionService/RejectPromotion` | `sis.promotion.reject_promotion` |  | Reject (student detained). |
| 484 | `POST /v1/sis/promotions:apply` | `PromotionService/ApplyPromotions` | `sis.promotion.apply_promotions` |  | Create next-year enrolments for approved promotions and close the current ones. |
| 485 | `POST /v1/sis/subject-registrations/{subjectRegistrationId}:submit` | `SubjectRegistrationService/SubmitSubjectRegistration` | `sis.subject_registration.submit_subject_registration` |  | Submit for approval. |
| 486 | `POST /v1/sis/subject-registrations/{subjectRegistrationId}:approve` | `SubjectRegistrationService/ApproveSubjectRegistration` | `sis.subject_registration.approve_subject_registration` |  | Approve; subjects are added to the registration. |
| 487 | `POST /v1/sis/subject-registrations/{subjectRegistrationId}:reject` | `SubjectRegistrationService/RejectSubjectRegistration` | `sis.subject_registration.reject_subject_registration` |  | Reject. |
| 488 | `POST /v1/sis/subject-registrations/{subjectRegistrationId}:resetToDraft` | `SubjectRegistrationService/ResetSubjectRegistration` | `sis.subject_registration.reset_subject_registration` |  | Back to draft. |
| 489 | `POST /v1/sis/subject-registrations/{subjectRegistrationId}:loadCourseSubjects` | `SubjectRegistrationService/LoadCourseSubjects` | `sis.subject_registration.load_course_subjects` |  | Pre-fill compulsory/elective lines from the course subjects. |
| 490 | `POST /v1/sis/discipline-incidents/{disciplineIncidentId}:markActionTaken` | `DisciplineIncidentService/MarkIncidentActionTaken` | `sis.discipline_incident.mark_incident_action_taken` |  | Record that action was taken. |
| 491 | `POST /v1/sis/discipline-incidents/{disciplineIncidentId}:markSeen` | `DisciplineIncidentService/MarkIncidentSeen` | `sis.discipline_incident.mark_incident_seen` |  | Guardian acknowledges the incident. |
| 492 | `GET /v1/sis/class-sections/{classSectionId}:roster` | `StudentEnrollmentService/ListClassRoster` | `sis.student_enrollment.list_class_roster` |  | Students placed in the class. |
| 493 | `GET /v1/sis/student-leave-requests` | `StudentLeaveRequestService/ListStudentLeaveRequests` | `sis.student_leave_request.read` |  | List StudentLeaveRequests. |
| 494 | `POST /v1/sis/student-leave-requests` | `StudentLeaveRequestService/CreateStudentLeaveRequest` | `sis.student_leave_request.create` |  | Create StudentLeaveRequest. |
| 495 | `GET /v1/sis/student-leave-requests/{studentLeaveRequestId}` | `StudentLeaveRequestService/GetStudentLeaveRequest` | `sis.student_leave_request.read` |  | Get StudentLeaveRequest. |
| 496 | `PATCH /v1/sis/student-leave-requests/{studentLeaveRequestId}` | `StudentLeaveRequestService/UpdateStudentLeaveRequest` | `sis.student_leave_request.update` |  | Update StudentLeaveRequest. |
| 497 | `DELETE /v1/sis/student-leave-requests/{studentLeaveRequestId}` | `StudentLeaveRequestService/DeleteStudentLeaveRequest` | `sis.student_leave_request.delete` |  | Delete StudentLeaveRequest. |
| 498 | `POST /v1/sis/student-leave-requests/{studentLeaveRequestId}:approve` | `StudentLeaveRequestService/ApproveStudentLeaveRequest` | `sis.student_leave_request.approve` |  | Approve a student leave request. |
| 499 | `POST /v1/sis/student-leave-requests/{studentLeaveRequestId}:reject` | `StudentLeaveRequestService/RejectStudentLeaveRequest` | `sis.student_leave_request.reject` |  | Reject a student leave request. |
| 500 | `GET /v1/sis/operations` | `OperationService/ListOperations` | `sis.operation.read` |  | Long-running operations of this module started by the caller. |
| 501 | `GET /v1/sis/operations/{operationId}` | `OperationService/GetOperation` | `sis.operation.read` |  | Status of a long-running operation started in this module. |
| 502 | `POST /v1/sis/operations/{operationId}:cancel` | `OperationService/CancelOperation` | `sis.operation.cancel_operation` |  | Request cancellation of a running operation. |

## Workforce / HR (`hr`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 503 | `GET /v1/hr/designations` | `DesignationService/ListDesignations` | `hr.designation.read` |  | List Designations. |
| 504 | `POST /v1/hr/designations` | `DesignationService/CreateDesignation` | `hr.designation.create` |  | Create Designation. |
| 505 | `GET /v1/hr/designations/{designationId}` | `DesignationService/GetDesignation` | `hr.designation.read` |  | Get Designation. |
| 506 | `PATCH /v1/hr/designations/{designationId}` | `DesignationService/UpdateDesignation` | `hr.designation.update` |  | Update Designation. |
| 507 | `DELETE /v1/hr/designations/{designationId}` | `DesignationService/DeleteDesignation` | `hr.designation.delete` |  | Delete Designation. |
| 508 | `GET /v1/hr/staff` | `StaffService/ListStaff` | `hr.staff.read` |  | List Staff. |
| 509 | `POST /v1/hr/staff` | `StaffService/CreateStaff` | `hr.staff.create` |  | Create Staff. |
| 510 | `GET /v1/hr/staff/{staffId}` | `StaffService/GetStaff` | `hr.staff.read` |  | Get Staff. |
| 511 | `PATCH /v1/hr/staff/{staffId}` | `StaffService/UpdateStaff` | `hr.staff.update` |  | Update Staff. |
| 512 | `DELETE /v1/hr/staff/{staffId}` | `StaffService/DeleteStaff` | `hr.staff.delete` |  | Delete Staff. |
| 513 | `GET /v1/hr/staff/{staffId}/departments` | `StaffService/ListStaffDepartments` | `hr.staff_department.read` |  | List StaffDepartments. |
| 514 | `POST /v1/hr/staff/{staffId}/departments` | `StaffService/CreateStaffDepartment` | `hr.staff_department.create` |  | Create StaffDepartment. |
| 515 | `GET /v1/hr/staff/{staffId}/departments/{staffDepartmentId}` | `StaffService/GetStaffDepartment` | `hr.staff_department.read` |  | Get StaffDepartment. |
| 516 | `PATCH /v1/hr/staff/{staffId}/departments/{staffDepartmentId}` | `StaffService/UpdateStaffDepartment` | `hr.staff_department.update` |  | Update StaffDepartment. |
| 517 | `DELETE /v1/hr/staff/{staffId}/departments/{staffDepartmentId}` | `StaffService/DeleteStaffDepartment` | `hr.staff_department.delete` |  | Delete StaffDepartment. |
| 518 | `GET /v1/hr/staff/{staffId}/subjects` | `StaffService/ListStaffSubjects` | `hr.staff_subject.read` |  | List StaffSubjects. |
| 519 | `POST /v1/hr/staff/{staffId}/subjects` | `StaffService/CreateStaffSubject` | `hr.staff_subject.create` |  | Create StaffSubject. |
| 520 | `GET /v1/hr/staff/{staffId}/subjects/{staffSubjectId}` | `StaffService/GetStaffSubject` | `hr.staff_subject.read` |  | Get StaffSubject. |
| 521 | `PATCH /v1/hr/staff/{staffId}/subjects/{staffSubjectId}` | `StaffService/UpdateStaffSubject` | `hr.staff_subject.update` |  | Update StaffSubject. |
| 522 | `DELETE /v1/hr/staff/{staffId}/subjects/{staffSubjectId}` | `StaffService/DeleteStaffSubject` | `hr.staff_subject.delete` |  | Delete StaffSubject. |
| 523 | `GET /v1/hr/leave-types` | `LeaveTypeService/ListLeaveTypes` | `hr.leave_type.read` |  | List LeaveTypes. |
| 524 | `POST /v1/hr/leave-types` | `LeaveTypeService/CreateLeaveType` | `hr.leave_type.create` |  | Create LeaveType. |
| 525 | `GET /v1/hr/leave-types/{leaveTypeId}` | `LeaveTypeService/GetLeaveType` | `hr.leave_type.read` |  | Get LeaveType. |
| 526 | `PATCH /v1/hr/leave-types/{leaveTypeId}` | `LeaveTypeService/UpdateLeaveType` | `hr.leave_type.update` |  | Update LeaveType. |
| 527 | `DELETE /v1/hr/leave-types/{leaveTypeId}` | `LeaveTypeService/DeleteLeaveType` | `hr.leave_type.delete` |  | Delete LeaveType. |
| 528 | `GET /v1/hr/leave-applications` | `LeaveApplicationService/ListLeaveApplications` | `hr.leave_application.read` |  | List LeaveApplications. |
| 529 | `POST /v1/hr/leave-applications` | `LeaveApplicationService/CreateLeaveApplication` | `hr.leave_application.create` |  | Create LeaveApplication. |
| 530 | `GET /v1/hr/leave-applications/{leaveApplicationId}` | `LeaveApplicationService/GetLeaveApplication` | `hr.leave_application.read` |  | Get LeaveApplication. |
| 531 | `PATCH /v1/hr/leave-applications/{leaveApplicationId}` | `LeaveApplicationService/UpdateLeaveApplication` | `hr.leave_application.update` |  | Update LeaveApplication. |
| 532 | `DELETE /v1/hr/leave-applications/{leaveApplicationId}` | `LeaveApplicationService/DeleteLeaveApplication` | `hr.leave_application.delete` |  | Delete LeaveApplication. |
| 533 | `POST /v1/hr/staff/{staffId}:changeStatus` | `StaffService/ChangeStaffStatus` | `hr.staff.change_staff_status` |  | Activate, deactivate or exit. |
| 534 | `POST /v1/hr/staff/{staffId}:createPortalAccount` | `StaffService/CreateStaffPortalAccount` | `hr.staff.create_staff_portal_account` |  | Create the staff login account. |
| 535 | `GET /v1/hr/staff/{staffId}:leaveBalance` | `StaffService/GetStaffLeaveBalance` | `hr.staff.get_leave_balance` |  | Entitlement, taken, pending and available days per leave type. |
| 536 | `POST /v1/hr/leave-applications/{leaveApplicationId}:approve` | `LeaveApplicationService/ApproveLeave` | `hr.leave_application.approve_leave` |  | Approve. |
| 537 | `POST /v1/hr/leave-applications/{leaveApplicationId}:reject` | `LeaveApplicationService/RejectLeave` | `hr.leave_application.reject_leave` |  | Reject. |
| 538 | `POST /v1/hr/leave-applications/{leaveApplicationId}:cancel` | `LeaveApplicationService/CancelLeave` | `hr.leave_application.cancel_leave` |  | Withdraw by applicant. |
| 539 | `GET /v1/hr/leave-balances` | `LeaveBalanceService/ListLeaveBalances` | `hr.leave_balance.read` |  | List LeaveBalances. |
| 540 | `POST /v1/hr/leave-balances` | `LeaveBalanceService/CreateLeaveBalance` | `hr.leave_balance.create` |  | Create LeaveBalance. |
| 541 | `GET /v1/hr/leave-balances/{leaveBalanceId}` | `LeaveBalanceService/GetLeaveBalance` | `hr.leave_balance.read` |  | Get LeaveBalance. |
| 542 | `PATCH /v1/hr/leave-balances/{leaveBalanceId}` | `LeaveBalanceService/UpdateLeaveBalance` | `hr.leave_balance.update` |  | Update LeaveBalance. |

## Payroll (`payroll`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 543 | `GET /v1/payroll/pay-components` | `PayComponentService/ListPayComponents` | `payroll.pay_component.read` |  | List PayComponents. |
| 544 | `POST /v1/payroll/pay-components` | `PayComponentService/CreatePayComponent` | `payroll.pay_component.create` |  | Create PayComponent. |
| 545 | `GET /v1/payroll/pay-components/{payComponentId}` | `PayComponentService/GetPayComponent` | `payroll.pay_component.read` |  | Get PayComponent. |
| 546 | `PATCH /v1/payroll/pay-components/{payComponentId}` | `PayComponentService/UpdatePayComponent` | `payroll.pay_component.update` |  | Update PayComponent. |
| 547 | `DELETE /v1/payroll/pay-components/{payComponentId}` | `PayComponentService/DeletePayComponent` | `payroll.pay_component.delete` |  | Delete PayComponent. |
| 548 | `GET /v1/payroll/pay-categories` | `PayCategoryService/ListPayCategories` | `payroll.pay_category.read` |  | List PayCategories. |
| 549 | `POST /v1/payroll/pay-categories` | `PayCategoryService/CreatePayCategory` | `payroll.pay_category.create` |  | Create PayCategory. |
| 550 | `GET /v1/payroll/pay-categories/{payCategoryId}` | `PayCategoryService/GetPayCategory` | `payroll.pay_category.read` |  | Get PayCategory. |
| 551 | `PATCH /v1/payroll/pay-categories/{payCategoryId}` | `PayCategoryService/UpdatePayCategory` | `payroll.pay_category.update` |  | Update PayCategory. |
| 552 | `DELETE /v1/payroll/pay-categories/{payCategoryId}` | `PayCategoryService/DeletePayCategory` | `payroll.pay_category.delete` |  | Delete PayCategory. |
| 553 | `GET /v1/payroll/salary-templates` | `SalaryTemplateService/ListSalaryTemplates` | `payroll.salary_template.read` |  | List SalaryTemplates. |
| 554 | `POST /v1/payroll/salary-templates` | `SalaryTemplateService/CreateSalaryTemplate` | `payroll.salary_template.create` |  | Create SalaryTemplate. |
| 555 | `GET /v1/payroll/salary-templates/{salaryTemplateId}` | `SalaryTemplateService/GetSalaryTemplate` | `payroll.salary_template.read` |  | Get SalaryTemplate. |
| 556 | `PATCH /v1/payroll/salary-templates/{salaryTemplateId}` | `SalaryTemplateService/UpdateSalaryTemplate` | `payroll.salary_template.update` |  | Update SalaryTemplate. |
| 557 | `DELETE /v1/payroll/salary-templates/{salaryTemplateId}` | `SalaryTemplateService/DeleteSalaryTemplate` | `payroll.salary_template.delete` |  | Delete SalaryTemplate. |
| 558 | `GET /v1/payroll/salary-templates/{salaryTemplateId}/components` | `SalaryTemplateService/ListSalaryTemplateComponents` | `payroll.salary_template_component.read` |  | List SalaryTemplateComponents. |
| 559 | `POST /v1/payroll/salary-templates/{salaryTemplateId}/components` | `SalaryTemplateService/CreateSalaryTemplateComponent` | `payroll.salary_template_component.create` |  | Create SalaryTemplateComponent. |
| 560 | `GET /v1/payroll/salary-templates/{salaryTemplateId}/components/{salaryTemplateComponentId}` | `SalaryTemplateService/GetSalaryTemplateComponent` | `payroll.salary_template_component.read` |  | Get SalaryTemplateComponent. |
| 561 | `PATCH /v1/payroll/salary-templates/{salaryTemplateId}/components/{salaryTemplateComponentId}` | `SalaryTemplateService/UpdateSalaryTemplateComponent` | `payroll.salary_template_component.update` |  | Update SalaryTemplateComponent. |
| 562 | `DELETE /v1/payroll/salary-templates/{salaryTemplateId}/components/{salaryTemplateComponentId}` | `SalaryTemplateService/DeleteSalaryTemplateComponent` | `payroll.salary_template_component.delete` |  | Delete SalaryTemplateComponent. |
| 563 | `GET /v1/payroll/staff-salaries` | `StaffSalaryService/ListStaffSalaries` | `payroll.staff_salary.read` |  | List StaffSalaries. |
| 564 | `POST /v1/payroll/staff-salaries` | `StaffSalaryService/CreateStaffSalary` | `payroll.staff_salary.create` |  | Create StaffSalary. |
| 565 | `GET /v1/payroll/staff-salaries/{staffSalaryId}` | `StaffSalaryService/GetStaffSalary` | `payroll.staff_salary.read` |  | Get StaffSalary. |
| 566 | `PATCH /v1/payroll/staff-salaries/{staffSalaryId}` | `StaffSalaryService/UpdateStaffSalary` | `payroll.staff_salary.update` |  | Update StaffSalary. |
| 567 | `DELETE /v1/payroll/staff-salaries/{staffSalaryId}` | `StaffSalaryService/DeleteStaffSalary` | `payroll.staff_salary.delete` |  | Delete StaffSalary. |
| 568 | `GET /v1/payroll/staff-salaries/{staffSalaryId}/components` | `StaffSalaryService/ListStaffSalaryComponents` | `payroll.staff_salary_component.read` |  | List StaffSalaryComponents. |
| 569 | `POST /v1/payroll/staff-salaries/{staffSalaryId}/components` | `StaffSalaryService/CreateStaffSalaryComponent` | `payroll.staff_salary_component.create` |  | Create StaffSalaryComponent. |
| 570 | `GET /v1/payroll/staff-salaries/{staffSalaryId}/components/{staffSalaryComponentId}` | `StaffSalaryService/GetStaffSalaryComponent` | `payroll.staff_salary_component.read` |  | Get StaffSalaryComponent. |
| 571 | `PATCH /v1/payroll/staff-salaries/{staffSalaryId}/components/{staffSalaryComponentId}` | `StaffSalaryService/UpdateStaffSalaryComponent` | `payroll.staff_salary_component.update` |  | Update StaffSalaryComponent. |
| 572 | `DELETE /v1/payroll/staff-salaries/{staffSalaryId}/components/{staffSalaryComponentId}` | `StaffSalaryService/DeleteStaffSalaryComponent` | `payroll.staff_salary_component.delete` |  | Delete StaffSalaryComponent. |
| 573 | `GET /v1/payroll/payslips` | `PayslipService/ListPayslips` | `payroll.payslip.read` |  | List Payslips. |
| 574 | `GET /v1/payroll/payslips/{payslipId}` | `PayslipService/GetPayslip` | `payroll.payslip.read` |  | Get Payslip. |
| 575 | `GET /v1/payroll/payslips/{payslipId}/lines` | `PayslipService/ListPayslipLines` | `payroll.payslip_line.read` |  | List PayslipLines. |
| 576 | `GET /v1/payroll/payslips/{payslipId}/lines/{payslipLineId}` | `PayslipService/GetPayslipLine` | `payroll.payslip_line.read` |  | Get PayslipLine. |
| 577 | `GET /v1/payroll/payment-types` | `PaymentTypeService/ListPaymentTypes` | `payroll.payment_type.read` |  | List PaymentTypes. |
| 578 | `POST /v1/payroll/payment-types` | `PaymentTypeService/CreatePaymentType` | `payroll.payment_type.create` |  | Create PaymentType. |
| 579 | `GET /v1/payroll/payment-types/{paymentTypeId}` | `PaymentTypeService/GetPaymentType` | `payroll.payment_type.read` |  | Get PaymentType. |
| 580 | `PATCH /v1/payroll/payment-types/{paymentTypeId}` | `PaymentTypeService/UpdatePaymentType` | `payroll.payment_type.update` |  | Update PaymentType. |
| 581 | `DELETE /v1/payroll/payment-types/{paymentTypeId}` | `PaymentTypeService/DeletePaymentType` | `payroll.payment_type.delete` |  | Delete PaymentType. |
| 582 | `GET /v1/payroll/disbursement-accounts` | `DisbursementAccountService/ListDisbursementAccounts` | `payroll.disbursement_account.read` |  | List DisbursementAccounts. |
| 583 | `POST /v1/payroll/disbursement-accounts` | `DisbursementAccountService/CreateDisbursementAccount` | `payroll.disbursement_account.create` |  | Create DisbursementAccount. |
| 584 | `GET /v1/payroll/disbursement-accounts/{disbursementAccountId}` | `DisbursementAccountService/GetDisbursementAccount` | `payroll.disbursement_account.read` |  | Get DisbursementAccount. |
| 585 | `PATCH /v1/payroll/disbursement-accounts/{disbursementAccountId}` | `DisbursementAccountService/UpdateDisbursementAccount` | `payroll.disbursement_account.update` |  | Update DisbursementAccount. |
| 586 | `DELETE /v1/payroll/disbursement-accounts/{disbursementAccountId}` | `DisbursementAccountService/DeleteDisbursementAccount` | `payroll.disbursement_account.delete` |  | Delete DisbursementAccount. |
| 587 | `GET /v1/payroll/payroll-payments` | `PayrollPaymentService/ListPayrollPayments` | `payroll.payroll_payment.read` |  | List PayrollPayments. |
| 588 | `POST /v1/payroll/payroll-payments` | `PayrollPaymentService/CreatePayrollPayment` | `payroll.payroll_payment.create` |  | Create PayrollPayment. |
| 589 | `GET /v1/payroll/payroll-payments/{payrollPaymentId}` | `PayrollPaymentService/GetPayrollPayment` | `payroll.payroll_payment.read` |  | Get PayrollPayment. |
| 590 | `POST /v1/payroll/payslips:generate` | `PayslipService/GeneratePayslips` | `payroll.payslip.generate_payslips` |  | Generate payslips from staff salaries, leave and attendance. |
| 591 | `POST /v1/payroll/payslips/{payslipId}:markPaid` | `PayslipService/MarkPayslipPaid` | `payroll.payslip.mark_payslip_paid` |  | Mark paid and create the payroll payment. |
| 592 | `POST /v1/payroll/payslips/{payslipId}:cancel` | `PayslipService/CancelPayslip` | `payroll.payslip.cancel_payslip` |  | Cancel. |
| 593 | `GET /v1/payroll/staff-bank-accounts` | `StaffBankAccountService/ListStaffBankAccounts` | `payroll.staff_bank_account.read` |  | List StaffBankAccounts. |
| 594 | `POST /v1/payroll/staff-bank-accounts` | `StaffBankAccountService/CreateStaffBankAccount` | `payroll.staff_bank_account.create` |  | Create StaffBankAccount. |
| 595 | `GET /v1/payroll/staff-bank-accounts/{staffBankAccountId}` | `StaffBankAccountService/GetStaffBankAccount` | `payroll.staff_bank_account.read` |  | Get StaffBankAccount. |
| 596 | `PATCH /v1/payroll/staff-bank-accounts/{staffBankAccountId}` | `StaffBankAccountService/UpdateStaffBankAccount` | `payroll.staff_bank_account.update` |  | Update StaffBankAccount. |
| 597 | `DELETE /v1/payroll/staff-bank-accounts/{staffBankAccountId}` | `StaffBankAccountService/DeleteStaffBankAccount` | `payroll.staff_bank_account.delete` |  | Delete StaffBankAccount. |
| 598 | `GET /v1/payroll/operations` | `OperationService/ListOperations` | `payroll.operation.read` |  | Long-running operations of this module started by the caller. |
| 599 | `GET /v1/payroll/operations/{operationId}` | `OperationService/GetOperation` | `payroll.operation.read` |  | Status of a long-running operation started in this module. |
| 600 | `POST /v1/payroll/operations/{operationId}:cancel` | `OperationService/CancelOperation` | `payroll.operation.cancel_operation` |  | Request cancellation of a running operation. |

## Admissions (`admission`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 601 | `GET /v1/admission/admission-cycles` | `AdmissionCycleService/ListAdmissionCycles` | `admission.admission_cycle.read` |  | List AdmissionCycles. |
| 602 | `POST /v1/admission/admission-cycles` | `AdmissionCycleService/CreateAdmissionCycle` | `admission.admission_cycle.create` |  | Create AdmissionCycle. |
| 603 | `GET /v1/admission/admission-cycles/{admissionCycleId}` | `AdmissionCycleService/GetAdmissionCycle` | `admission.admission_cycle.read` |  | Get AdmissionCycle. |
| 604 | `PATCH /v1/admission/admission-cycles/{admissionCycleId}` | `AdmissionCycleService/UpdateAdmissionCycle` | `admission.admission_cycle.update` |  | Update AdmissionCycle. |
| 605 | `DELETE /v1/admission/admission-cycles/{admissionCycleId}` | `AdmissionCycleService/DeleteAdmissionCycle` | `admission.admission_cycle.delete` |  | Delete AdmissionCycle. |
| 606 | `GET /v1/admission/admission-cycles/{admissionCycleId}/fees` | `AdmissionCycleService/ListAdmissionCycleFees` | `admission.admission_cycle_fee.read` |  | List AdmissionCycleFees. |
| 607 | `POST /v1/admission/admission-cycles/{admissionCycleId}/fees` | `AdmissionCycleService/CreateAdmissionCycleFee` | `admission.admission_cycle_fee.create` |  | Create AdmissionCycleFee. |
| 608 | `GET /v1/admission/admission-cycles/{admissionCycleId}/fees/{admissionCycleFeeId}` | `AdmissionCycleService/GetAdmissionCycleFee` | `admission.admission_cycle_fee.read` |  | Get AdmissionCycleFee. |
| 609 | `PATCH /v1/admission/admission-cycles/{admissionCycleId}/fees/{admissionCycleFeeId}` | `AdmissionCycleService/UpdateAdmissionCycleFee` | `admission.admission_cycle_fee.update` |  | Update AdmissionCycleFee. |
| 610 | `DELETE /v1/admission/admission-cycles/{admissionCycleId}/fees/{admissionCycleFeeId}` | `AdmissionCycleService/DeleteAdmissionCycleFee` | `admission.admission_cycle_fee.delete` |  | Delete AdmissionCycleFee. |
| 611 | `GET /v1/admission/applications` | `ApplicationService/ListApplications` | `admission.application.read` |  | List Applications. |
| 612 | `POST /v1/admission/applications` | `ApplicationService/CreateApplication` | `admission.application.create` |  | Create Application. |
| 613 | `GET /v1/admission/applications/{applicationId}` | `ApplicationService/GetApplication` | `admission.application.read` |  | Get Application. |
| 614 | `PATCH /v1/admission/applications/{applicationId}` | `ApplicationService/UpdateApplication` | `admission.application.update` |  | Update Application. |
| 615 | `DELETE /v1/admission/applications/{applicationId}` | `ApplicationService/DeleteApplication` | `admission.application.delete` |  | Delete Application. |
| 616 | `GET /v1/admission/applications/{applicationId}/guardians` | `ApplicationService/ListApplicationGuardians` | `admission.application_guardian.read` |  | List ApplicationGuardians. |
| 617 | `POST /v1/admission/applications/{applicationId}/guardians` | `ApplicationService/CreateApplicationGuardian` | `admission.application_guardian.create` |  | Create ApplicationGuardian. |
| 618 | `GET /v1/admission/applications/{applicationId}/guardians/{applicationGuardianId}` | `ApplicationService/GetApplicationGuardian` | `admission.application_guardian.read` |  | Get ApplicationGuardian. |
| 619 | `PATCH /v1/admission/applications/{applicationId}/guardians/{applicationGuardianId}` | `ApplicationService/UpdateApplicationGuardian` | `admission.application_guardian.update` |  | Update ApplicationGuardian. |
| 620 | `DELETE /v1/admission/applications/{applicationId}/guardians/{applicationGuardianId}` | `ApplicationService/DeleteApplicationGuardian` | `admission.application_guardian.delete` |  | Delete ApplicationGuardian. |
| 621 | `GET /v1/admission/applications/{applicationId}/emergency-contacts` | `ApplicationService/ListApplicationEmergencyContacts` | `admission.application_emergency_contact.read` |  | List ApplicationEmergencyContacts. |
| 622 | `POST /v1/admission/applications/{applicationId}/emergency-contacts` | `ApplicationService/CreateApplicationEmergencyContact` | `admission.application_emergency_contact.create` |  | Create ApplicationEmergencyContact. |
| 623 | `GET /v1/admission/applications/{applicationId}/emergency-contacts/{applicationEmergencyContactId}` | `ApplicationService/GetApplicationEmergencyContact` | `admission.application_emergency_contact.read` |  | Get ApplicationEmergencyContact. |
| 624 | `PATCH /v1/admission/applications/{applicationId}/emergency-contacts/{applicationEmergencyContactId}` | `ApplicationService/UpdateApplicationEmergencyContact` | `admission.application_emergency_contact.update` |  | Update ApplicationEmergencyContact. |
| 625 | `DELETE /v1/admission/applications/{applicationId}/emergency-contacts/{applicationEmergencyContactId}` | `ApplicationService/DeleteApplicationEmergencyContact` | `admission.application_emergency_contact.delete` |  | Delete ApplicationEmergencyContact. |
| 626 | `POST /v1/admission/admission-cycles/{admissionCycleId}:confirm` | `AdmissionCycleService/ConfirmAdmissionCycle` | `admission.admission_cycle.confirm_admission_cycle` |  | Confirm the cycle. |
| 627 | `POST /v1/admission/admission-cycles/{admissionCycleId}:openApplications` | `AdmissionCycleService/OpenApplications` | `admission.admission_cycle.open_applications` |  | Start accepting applications. |
| 628 | `POST /v1/admission/admission-cycles/{admissionCycleId}:startAdmission` | `AdmissionCycleService/StartAdmission` | `admission.admission_cycle.start_admission` |  | Move to the admission stage. |
| 629 | `POST /v1/admission/admission-cycles/{admissionCycleId}:close` | `AdmissionCycleService/CloseAdmissionCycle` | `admission.admission_cycle.close_admission_cycle` |  | Close. |
| 630 | `POST /v1/admission/admission-cycles/{admissionCycleId}:cancel` | `AdmissionCycleService/CancelAdmissionCycle` | `admission.admission_cycle.cancel_admission_cycle` |  | Cancel. |
| 631 | `POST /v1/admission/admission-cycles/{admissionCycleId}:resetToDraft` | `AdmissionCycleService/ResetAdmissionCycle` | `admission.admission_cycle.reset_admission_cycle` |  | Back to draft. |
| 632 | `POST /v1/admission/applications/{applicationId}:submit` | `ApplicationService/SubmitApplication` | `admission.application.submit_application` |  | Submit (applicant or office). |
| 633 | `POST /v1/admission/applications/{applicationId}:confirm` | `ApplicationService/ConfirmApplication` | `admission.application.confirm_application` |  | Office confirms the application is complete. |
| 634 | `POST /v1/admission/applications/{applicationId}:markPending` | `ApplicationService/MarkApplicationPending` | `admission.application.mark_application_pending` |  | Put on hold. |
| 635 | `POST /v1/admission/applications/{applicationId}:reject` | `ApplicationService/RejectApplication` | `admission.application.reject_application` |  | Reject. |
| 636 | `POST /v1/admission/applications/{applicationId}:cancel` | `ApplicationService/CancelApplication` | `admission.application.cancel_application` |  | Cancel. |
| 637 | `POST /v1/admission/applications/{applicationId}:resetToDraft` | `ApplicationService/ResetApplication` | `admission.application.reset_application` |  | Back to draft. |
| 638 | `POST /v1/admission/applications/{applicationId}:approve` | `ApplicationService/ApproveApplication` | `admission.application.approve_application` |  | Approve after review / interview (status approved). |
| 639 | `POST /v1/admission/applications/{applicationId}:confirmAdmission` | `ApplicationService/ConfirmAdmission` | `admission.application.confirm_admission` |  | Seat offered and accepted (status admitted). |
| 640 | `POST /v1/admission/applications/{applicationId}:enroll` | `ApplicationService/EnrollApplicant` | `admission.application.enroll_applicant` |  | Create person, student, guardians, enrolment and fee dues from an admitted application; sets status done. |
| 641 | `GET /v1/admission/operations` | `OperationService/ListOperations` | `admission.operation.read` |  | Long-running operations of this module started by the caller. |
| 642 | `GET /v1/admission/operations/{operationId}` | `OperationService/GetOperation` | `admission.operation.read` |  | Status of a long-running operation started in this module. |
| 643 | `POST /v1/admission/operations/{operationId}:cancel` | `OperationService/CancelOperation` | `admission.operation.cancel_operation` |  | Request cancellation of a running operation. |

## Attendance (`attendance`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 644 | `GET /v1/attendance/attendance-types` | `AttendanceTypeService/ListAttendanceTypes` | `attendance.attendance_type.read` |  | List AttendanceTypes. |
| 645 | `POST /v1/attendance/attendance-types` | `AttendanceTypeService/CreateAttendanceType` | `attendance.attendance_type.create` |  | Create AttendanceType. |
| 646 | `GET /v1/attendance/attendance-types/{attendanceTypeId}` | `AttendanceTypeService/GetAttendanceType` | `attendance.attendance_type.read` |  | Get AttendanceType. |
| 647 | `PATCH /v1/attendance/attendance-types/{attendanceTypeId}` | `AttendanceTypeService/UpdateAttendanceType` | `attendance.attendance_type.update` |  | Update AttendanceType. |
| 648 | `DELETE /v1/attendance/attendance-types/{attendanceTypeId}` | `AttendanceTypeService/DeleteAttendanceType` | `attendance.attendance_type.delete` |  | Delete AttendanceType. |
| 649 | `GET /v1/attendance/absence-reasons` | `AbsenceReasonService/ListAbsenceReasons` | `attendance.absence_reason.read` |  | List AbsenceReasons. |
| 650 | `POST /v1/attendance/absence-reasons` | `AbsenceReasonService/CreateAbsenceReason` | `attendance.absence_reason.create` |  | Create AbsenceReason. |
| 651 | `GET /v1/attendance/absence-reasons/{absenceReasonId}` | `AbsenceReasonService/GetAbsenceReason` | `attendance.absence_reason.read` |  | Get AbsenceReason. |
| 652 | `PATCH /v1/attendance/absence-reasons/{absenceReasonId}` | `AbsenceReasonService/UpdateAbsenceReason` | `attendance.absence_reason.update` |  | Update AbsenceReason. |
| 653 | `DELETE /v1/attendance/absence-reasons/{absenceReasonId}` | `AbsenceReasonService/DeleteAbsenceReason` | `attendance.absence_reason.delete` |  | Delete AbsenceReason. |
| 654 | `GET /v1/attendance/attendance-registers` | `AttendanceRegisterService/ListAttendanceRegisters` | `attendance.attendance_register.read` |  | List AttendanceRegisters. |
| 655 | `POST /v1/attendance/attendance-registers` | `AttendanceRegisterService/CreateAttendanceRegister` | `attendance.attendance_register.create` |  | Create AttendanceRegister. |
| 656 | `GET /v1/attendance/attendance-registers/{attendanceRegisterId}` | `AttendanceRegisterService/GetAttendanceRegister` | `attendance.attendance_register.read` |  | Get AttendanceRegister. |
| 657 | `PATCH /v1/attendance/attendance-registers/{attendanceRegisterId}` | `AttendanceRegisterService/UpdateAttendanceRegister` | `attendance.attendance_register.update` |  | Update AttendanceRegister. |
| 658 | `DELETE /v1/attendance/attendance-registers/{attendanceRegisterId}` | `AttendanceRegisterService/DeleteAttendanceRegister` | `attendance.attendance_register.delete` |  | Delete AttendanceRegister. |
| 659 | `GET /v1/attendance/attendance-sheets` | `AttendanceSheetService/ListAttendanceSheets` | `attendance.attendance_sheet.read` |  | List AttendanceSheets. |
| 660 | `POST /v1/attendance/attendance-sheets` | `AttendanceSheetService/CreateAttendanceSheet` | `attendance.attendance_sheet.create` |  | Create AttendanceSheet. |
| 661 | `GET /v1/attendance/attendance-sheets/{attendanceSheetId}` | `AttendanceSheetService/GetAttendanceSheet` | `attendance.attendance_sheet.read` |  | Get AttendanceSheet. |
| 662 | `PATCH /v1/attendance/attendance-sheets/{attendanceSheetId}` | `AttendanceSheetService/UpdateAttendanceSheet` | `attendance.attendance_sheet.update` |  | Update AttendanceSheet. |
| 663 | `DELETE /v1/attendance/attendance-sheets/{attendanceSheetId}` | `AttendanceSheetService/DeleteAttendanceSheet` | `attendance.attendance_sheet.delete` |  | Delete AttendanceSheet. |
| 664 | `GET /v1/attendance/attendance-sheets/{attendanceSheetId}/records` | `AttendanceSheetService/ListAttendanceRecords` | `attendance.attendance_record.read` |  | List AttendanceRecords. |
| 665 | `GET /v1/attendance/attendance-sheets/{attendanceSheetId}/records/{attendanceRecordId}` | `AttendanceSheetService/GetAttendanceRecord` | `attendance.attendance_record.read` |  | Get AttendanceRecord. |
| 666 | `POST /v1/attendance/attendance-sheets/{attendanceSheetId}:start` | `AttendanceSheetService/StartAttendanceSheet` | `attendance.attendance_sheet.start_attendance_sheet` |  | Start taking attendance. |
| 667 | `POST /v1/attendance/attendance-sheets/{attendanceSheetId}:complete` | `AttendanceSheetService/CompleteAttendanceSheet` | `attendance.attendance_sheet.complete_attendance_sheet` |  | Attendance taken; guardians of absentees are notified. |
| 668 | `POST /v1/attendance/attendance-sheets/{attendanceSheetId}:cancel` | `AttendanceSheetService/CancelAttendanceSheet` | `attendance.attendance_sheet.cancel_attendance_sheet` |  | Cancel. |
| 669 | `POST /v1/attendance/attendance-sheets/{attendanceSheetId}:resetToDraft` | `AttendanceSheetService/ResetAttendanceSheet` | `attendance.attendance_sheet.reset_attendance_sheet` |  | Back to draft. |
| 670 | `POST /v1/attendance/attendance-sheets/{attendanceSheetId}:recordAttendance` | `AttendanceSheetService/RecordAttendance` | `attendance.attendance_sheet.record_attendance` |  | Record or correct marks for many students in one call. |
| 671 | `POST /v1/attendance/attendance-sheets:forSession` | `AttendanceSheetService/TakeAttendanceForSession` | `attendance.attendance_sheet.take_attendance_for_session` |  | Get or create the sheet for a timetable session (pre-filled with expected students). |
| 672 | `GET /v1/attendance/summaries` | `AttendanceSheetService/SummarizeAttendance` | `attendance.attendance_record.summarize_attendance` |  | Attendance counts and percentages. |
| 673 | `GET /v1/attendance/staff-attendance-days` | `StaffAttendanceService/ListStaffAttendances` | `attendance.staff_attendance.read` |  | List StaffAttendances. |
| 674 | `POST /v1/attendance/staff-attendance-days` | `StaffAttendanceService/CreateStaffAttendance` | `attendance.staff_attendance.create` |  | Create StaffAttendance. |
| 675 | `GET /v1/attendance/staff-attendance-days/{staffAttendanceId}` | `StaffAttendanceService/GetStaffAttendance` | `attendance.staff_attendance.read` |  | Get StaffAttendance. |
| 676 | `PATCH /v1/attendance/staff-attendance-days/{staffAttendanceId}` | `StaffAttendanceService/UpdateStaffAttendance` | `attendance.staff_attendance.update` |  | Update StaffAttendance. |

## Rooms & Facilities (`facility`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 677 | `GET /v1/facility/facilities` | `FacilityService/ListFacilities` | `facility.facility.read` |  | List Facilities. |
| 678 | `POST /v1/facility/facilities` | `FacilityService/CreateFacility` | `facility.facility.create` |  | Create Facility. |
| 679 | `GET /v1/facility/facilities/{facilityId}` | `FacilityService/GetFacility` | `facility.facility.read` |  | Get Facility. |
| 680 | `PATCH /v1/facility/facilities/{facilityId}` | `FacilityService/UpdateFacility` | `facility.facility.update` |  | Update Facility. |
| 681 | `DELETE /v1/facility/facilities/{facilityId}` | `FacilityService/DeleteFacility` | `facility.facility.delete` |  | Delete Facility. |
| 682 | `GET /v1/facility/rooms` | `RoomService/ListRooms` | `facility.room.read` |  | List Rooms. |
| 683 | `POST /v1/facility/rooms` | `RoomService/CreateRoom` | `facility.room.create` |  | Create Room. |
| 684 | `GET /v1/facility/rooms/{roomId}` | `RoomService/GetRoom` | `facility.room.read` |  | Get Room. |
| 685 | `PATCH /v1/facility/rooms/{roomId}` | `RoomService/UpdateRoom` | `facility.room.update` |  | Update Room. |
| 686 | `DELETE /v1/facility/rooms/{roomId}` | `RoomService/DeleteRoom` | `facility.room.delete` |  | Delete Room. |
| 687 | `GET /v1/facility/rooms/{roomId}/facilities` | `RoomService/ListRoomFacilities` | `facility.room_facility.read` |  | List RoomFacilities. |
| 688 | `POST /v1/facility/rooms/{roomId}/facilities` | `RoomService/CreateRoomFacility` | `facility.room_facility.create` |  | Create RoomFacility. |
| 689 | `GET /v1/facility/rooms/{roomId}/facilities/{roomFacilityId}` | `RoomService/GetRoomFacility` | `facility.room_facility.read` |  | Get RoomFacility. |
| 690 | `PATCH /v1/facility/rooms/{roomId}/facilities/{roomFacilityId}` | `RoomService/UpdateRoomFacility` | `facility.room_facility.update` |  | Update RoomFacility. |
| 691 | `DELETE /v1/facility/rooms/{roomId}/facilities/{roomFacilityId}` | `RoomService/DeleteRoomFacility` | `facility.room_facility.delete` |  | Delete RoomFacility. |
| 692 | `GET /v1/facility/asset-items` | `AssetItemService/ListAssetItems` | `facility.asset_item.read` |  | List AssetItems. |
| 693 | `POST /v1/facility/asset-items` | `AssetItemService/CreateAssetItem` | `facility.asset_item.create` |  | Create AssetItem. |
| 694 | `GET /v1/facility/asset-items/{assetItemId}` | `AssetItemService/GetAssetItem` | `facility.asset_item.read` |  | Get AssetItem. |
| 695 | `PATCH /v1/facility/asset-items/{assetItemId}` | `AssetItemService/UpdateAssetItem` | `facility.asset_item.update` |  | Update AssetItem. |
| 696 | `DELETE /v1/facility/asset-items/{assetItemId}` | `AssetItemService/DeleteAssetItem` | `facility.asset_item.delete` |  | Delete AssetItem. |
| 697 | `GET /v1/facility/rooms/{roomId}/assets` | `RoomService/ListRoomAssets` | `facility.room_asset.read` |  | List RoomAssets. |
| 698 | `POST /v1/facility/rooms/{roomId}/assets` | `RoomService/CreateRoomAsset` | `facility.room_asset.create` |  | Create RoomAsset. |
| 699 | `GET /v1/facility/rooms/{roomId}/assets/{roomAssetId}` | `RoomService/GetRoomAsset` | `facility.room_asset.read` |  | Get RoomAsset. |
| 700 | `PATCH /v1/facility/rooms/{roomId}/assets/{roomAssetId}` | `RoomService/UpdateRoomAsset` | `facility.room_asset.update` |  | Update RoomAsset. |
| 701 | `DELETE /v1/facility/rooms/{roomId}/assets/{roomAssetId}` | `RoomService/DeleteRoomAsset` | `facility.room_asset.delete` |  | Delete RoomAsset. |

## Timetable (`timetable`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 702 | `GET /v1/timetable/periods` | `PeriodService/ListPeriods` | `timetable.period.read` |  | List Periods. |
| 703 | `POST /v1/timetable/periods` | `PeriodService/CreatePeriod` | `timetable.period.create` |  | Create Period. |
| 704 | `GET /v1/timetable/periods/{periodId}` | `PeriodService/GetPeriod` | `timetable.period.read` |  | Get Period. |
| 705 | `PATCH /v1/timetable/periods/{periodId}` | `PeriodService/UpdatePeriod` | `timetable.period.update` |  | Update Period. |
| 706 | `DELETE /v1/timetable/periods/{periodId}` | `PeriodService/DeletePeriod` | `timetable.period.delete` |  | Delete Period. |
| 707 | `GET /v1/timetable/sessions` | `SessionService/ListSessions` | `timetable.session.read` |  | List Sessions. |
| 708 | `POST /v1/timetable/sessions` | `SessionService/CreateSession` | `timetable.session.create` |  | Create Session. |
| 709 | `GET /v1/timetable/sessions/{sessionId}` | `SessionService/GetSession` | `timetable.session.read` |  | Get Session. |
| 710 | `PATCH /v1/timetable/sessions/{sessionId}` | `SessionService/UpdateSession` | `timetable.session.update` |  | Update Session. |
| 711 | `DELETE /v1/timetable/sessions/{sessionId}` | `SessionService/DeleteSession` | `timetable.session.delete` |  | Delete Session. |
| 712 | `GET /v1/timetable/sessions/{sessionId}/students` | `SessionService/ListSessionStudents` | `timetable.session_student.read` |  | List SessionStudents. |
| 713 | `POST /v1/timetable/sessions/{sessionId}/students` | `SessionService/CreateSessionStudent` | `timetable.session_student.create` |  | Create SessionStudent. |
| 714 | `GET /v1/timetable/sessions/{sessionId}/students/{sessionStudentId}` | `SessionService/GetSessionStudent` | `timetable.session_student.read` |  | Get SessionStudent. |
| 715 | `PATCH /v1/timetable/sessions/{sessionId}/students/{sessionStudentId}` | `SessionService/UpdateSessionStudent` | `timetable.session_student.update` |  | Update SessionStudent. |
| 716 | `DELETE /v1/timetable/sessions/{sessionId}/students/{sessionStudentId}` | `SessionService/DeleteSessionStudent` | `timetable.session_student.delete` |  | Delete SessionStudent. |
| 717 | `POST /v1/timetable/sessions/{sessionId}:confirm` | `SessionService/ConfirmSession` | `timetable.session.confirm_session` |  | Confirm. |
| 718 | `POST /v1/timetable/sessions/{sessionId}:complete` | `SessionService/CompleteSession` | `timetable.session.complete_session` |  | Mark done. |
| 719 | `POST /v1/timetable/sessions/{sessionId}:cancel` | `SessionService/CancelSession` | `timetable.session.cancel_session` |  | Cancel. |
| 720 | `POST /v1/timetable/sessions/{sessionId}:resetToDraft` | `SessionService/ResetSession` | `timetable.session.reset_session` |  | Back to draft. |
| 721 | `POST /v1/timetable/sessions:batchConfirm` | `SessionService/BatchConfirmSessions` | `timetable.session.batch_confirm_sessions` |  | Confirm many draft sessions at once. |
| 722 | `POST /v1/timetable/sessions:generate` | `SessionService/GenerateSessions` | `timetable.session.generate_sessions` |  | Generate sessions from a weekly pattern; clashes (teacher, room, class) are reported. |
| 723 | `GET /v1/timetable/sessions:timetable` | `SessionService/GetTimetable` | `timetable.session.get_timetable` |  | Timetable view for a class, teacher, student or room. |
| 724 | `GET /v1/timetable/operations` | `OperationService/ListOperations` | `timetable.operation.read` |  | Long-running operations of this module started by the caller. |
| 725 | `GET /v1/timetable/operations/{operationId}` | `OperationService/GetOperation` | `timetable.operation.read` |  | Status of a long-running operation started in this module. |
| 726 | `POST /v1/timetable/operations/{operationId}:cancel` | `OperationService/CancelOperation` | `timetable.operation.cancel_operation` |  | Request cancellation of a running operation. |

## Assessment & Results (`assessment`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 727 | `GET /v1/assessment/homeworks` | `HomeworkService/ListHomeworks` | `assessment.homework.read` |  | List Homeworks. |
| 728 | `POST /v1/assessment/homeworks` | `HomeworkService/CreateHomework` | `assessment.homework.create` |  | Create Homework. |
| 729 | `GET /v1/assessment/homeworks/{homeworkId}` | `HomeworkService/GetHomework` | `assessment.homework.read` |  | Get Homework. |
| 730 | `PATCH /v1/assessment/homeworks/{homeworkId}` | `HomeworkService/UpdateHomework` | `assessment.homework.update` |  | Update Homework. |
| 731 | `DELETE /v1/assessment/homeworks/{homeworkId}` | `HomeworkService/DeleteHomework` | `assessment.homework.delete` |  | Delete Homework. |
| 732 | `GET /v1/assessment/homeworks/{homeworkId}/approvals` | `HomeworkService/ListHomeworkApprovals` | `assessment.homework_approval.read` |  | List HomeworkApprovals. |
| 733 | `GET /v1/assessment/homeworks/{homeworkId}/approvals/{homeworkApprovalId}` | `HomeworkService/GetHomeworkApproval` | `assessment.homework_approval.read` |  | Get HomeworkApproval. |
| 734 | `GET /v1/assessment/homework-submissions` | `HomeworkSubmissionService/ListHomeworkSubmissions` | `assessment.homework_submission.read` |  | List HomeworkSubmissions. |
| 735 | `POST /v1/assessment/homework-submissions` | `HomeworkSubmissionService/CreateHomeworkSubmission` | `assessment.homework_submission.create` |  | Create HomeworkSubmission. |
| 736 | `GET /v1/assessment/homework-submissions/{homeworkSubmissionId}` | `HomeworkSubmissionService/GetHomeworkSubmission` | `assessment.homework_submission.read` |  | Get HomeworkSubmission. |
| 737 | `PATCH /v1/assessment/homework-submissions/{homeworkSubmissionId}` | `HomeworkSubmissionService/UpdateHomeworkSubmission` | `assessment.homework_submission.update` |  | Update HomeworkSubmission. |
| 738 | `DELETE /v1/assessment/homework-submissions/{homeworkSubmissionId}` | `HomeworkSubmissionService/DeleteHomeworkSubmission` | `assessment.homework_submission.delete` |  | Delete HomeworkSubmission. |
| 739 | `GET /v1/assessment/assignment-types` | `AssignmentTypeService/ListAssignmentTypes` | `assessment.assignment_type.read` |  | List AssignmentTypes. |
| 740 | `POST /v1/assessment/assignment-types` | `AssignmentTypeService/CreateAssignmentType` | `assessment.assignment_type.create` |  | Create AssignmentType. |
| 741 | `GET /v1/assessment/assignment-types/{assignmentTypeId}` | `AssignmentTypeService/GetAssignmentType` | `assessment.assignment_type.read` |  | Get AssignmentType. |
| 742 | `PATCH /v1/assessment/assignment-types/{assignmentTypeId}` | `AssignmentTypeService/UpdateAssignmentType` | `assessment.assignment_type.update` |  | Update AssignmentType. |
| 743 | `DELETE /v1/assessment/assignment-types/{assignmentTypeId}` | `AssignmentTypeService/DeleteAssignmentType` | `assessment.assignment_type.delete` |  | Delete AssignmentType. |
| 744 | `GET /v1/assessment/assignments` | `AssignmentService/ListAssignments` | `assessment.assignment.read` |  | List Assignments. |
| 745 | `POST /v1/assessment/assignments` | `AssignmentService/CreateAssignment` | `assessment.assignment.create` |  | Create Assignment. |
| 746 | `GET /v1/assessment/assignments/{assignmentId}` | `AssignmentService/GetAssignment` | `assessment.assignment.read` |  | Get Assignment. |
| 747 | `PATCH /v1/assessment/assignments/{assignmentId}` | `AssignmentService/UpdateAssignment` | `assessment.assignment.update` |  | Update Assignment. |
| 748 | `DELETE /v1/assessment/assignments/{assignmentId}` | `AssignmentService/DeleteAssignment` | `assessment.assignment.delete` |  | Delete Assignment. |
| 749 | `GET /v1/assessment/assignments/{assignmentId}/subjects` | `AssignmentService/ListAssignmentSubjects` | `assessment.assignment_subject.read` |  | List AssignmentSubjects. |
| 750 | `POST /v1/assessment/assignments/{assignmentId}/subjects` | `AssignmentService/CreateAssignmentSubject` | `assessment.assignment_subject.create` |  | Create AssignmentSubject. |
| 751 | `GET /v1/assessment/assignments/{assignmentId}/subjects/{assignmentSubjectId}` | `AssignmentService/GetAssignmentSubject` | `assessment.assignment_subject.read` |  | Get AssignmentSubject. |
| 752 | `PATCH /v1/assessment/assignments/{assignmentId}/subjects/{assignmentSubjectId}` | `AssignmentService/UpdateAssignmentSubject` | `assessment.assignment_subject.update` |  | Update AssignmentSubject. |
| 753 | `DELETE /v1/assessment/assignments/{assignmentId}/subjects/{assignmentSubjectId}` | `AssignmentService/DeleteAssignmentSubject` | `assessment.assignment_subject.delete` |  | Delete AssignmentSubject. |
| 754 | `GET /v1/assessment/assignments/{assignmentId}/allocations` | `AssignmentService/ListAssignmentAllocations` | `assessment.assignment_allocation.read` |  | List AssignmentAllocations. |
| 755 | `POST /v1/assessment/assignments/{assignmentId}/allocations` | `AssignmentService/CreateAssignmentAllocation` | `assessment.assignment_allocation.create` |  | Create AssignmentAllocation. |
| 756 | `GET /v1/assessment/assignments/{assignmentId}/allocations/{assignmentAllocationId}` | `AssignmentService/GetAssignmentAllocation` | `assessment.assignment_allocation.read` |  | Get AssignmentAllocation. |
| 757 | `PATCH /v1/assessment/assignments/{assignmentId}/allocations/{assignmentAllocationId}` | `AssignmentService/UpdateAssignmentAllocation` | `assessment.assignment_allocation.update` |  | Update AssignmentAllocation. |
| 758 | `DELETE /v1/assessment/assignments/{assignmentId}/allocations/{assignmentAllocationId}` | `AssignmentService/DeleteAssignmentAllocation` | `assessment.assignment_allocation.delete` |  | Delete AssignmentAllocation. |
| 759 | `GET /v1/assessment/assignments/{assignmentId}/approvals` | `AssignmentService/ListAssignmentApprovals` | `assessment.assignment_approval.read` |  | List AssignmentApprovals. |
| 760 | `POST /v1/assessment/assignments/{assignmentId}/approvals` | `AssignmentService/CreateAssignmentApproval` | `assessment.assignment_approval.create` |  | Create AssignmentApproval. |
| 761 | `GET /v1/assessment/assignments/{assignmentId}/approvals/{assignmentApprovalId}` | `AssignmentService/GetAssignmentApproval` | `assessment.assignment_approval.read` |  | Get AssignmentApproval. |
| 762 | `PATCH /v1/assessment/assignments/{assignmentId}/approvals/{assignmentApprovalId}` | `AssignmentService/UpdateAssignmentApproval` | `assessment.assignment_approval.update` |  | Update AssignmentApproval. |
| 763 | `DELETE /v1/assessment/assignments/{assignmentId}/approvals/{assignmentApprovalId}` | `AssignmentService/DeleteAssignmentApproval` | `assessment.assignment_approval.delete` |  | Delete AssignmentApproval. |
| 764 | `GET /v1/assessment/assignment-submissions` | `AssignmentSubmissionService/ListAssignmentSubmissions` | `assessment.assignment_submission.read` |  | List AssignmentSubmissions. |
| 765 | `POST /v1/assessment/assignment-submissions` | `AssignmentSubmissionService/CreateAssignmentSubmission` | `assessment.assignment_submission.create` |  | Create AssignmentSubmission. |
| 766 | `GET /v1/assessment/assignment-submissions/{assignmentSubmissionId}` | `AssignmentSubmissionService/GetAssignmentSubmission` | `assessment.assignment_submission.read` |  | Get AssignmentSubmission. |
| 767 | `PATCH /v1/assessment/assignment-submissions/{assignmentSubmissionId}` | `AssignmentSubmissionService/UpdateAssignmentSubmission` | `assessment.assignment_submission.update` |  | Update AssignmentSubmission. |
| 768 | `DELETE /v1/assessment/assignment-submissions/{assignmentSubmissionId}` | `AssignmentSubmissionService/DeleteAssignmentSubmission` | `assessment.assignment_submission.delete` |  | Delete AssignmentSubmission. |
| 769 | `GET /v1/assessment/grading-schemes` | `GradingSchemeService/ListGradingSchemes` | `assessment.grading_scheme.read` |  | List GradingSchemes. |
| 770 | `POST /v1/assessment/grading-schemes` | `GradingSchemeService/CreateGradingScheme` | `assessment.grading_scheme.create` |  | Create GradingScheme. |
| 771 | `GET /v1/assessment/grading-schemes/{gradingSchemeId}` | `GradingSchemeService/GetGradingScheme` | `assessment.grading_scheme.read` |  | Get GradingScheme. |
| 772 | `PATCH /v1/assessment/grading-schemes/{gradingSchemeId}` | `GradingSchemeService/UpdateGradingScheme` | `assessment.grading_scheme.update` |  | Update GradingScheme. |
| 773 | `DELETE /v1/assessment/grading-schemes/{gradingSchemeId}` | `GradingSchemeService/DeleteGradingScheme` | `assessment.grading_scheme.delete` |  | Delete GradingScheme. |
| 774 | `GET /v1/assessment/grading-bands` | `GradingBandService/ListGradingBands` | `assessment.grading_band.read` |  | List GradingBands. |
| 775 | `POST /v1/assessment/grading-bands` | `GradingBandService/CreateGradingBand` | `assessment.grading_band.create` |  | Create GradingBand. |
| 776 | `GET /v1/assessment/grading-bands/{gradingBandId}` | `GradingBandService/GetGradingBand` | `assessment.grading_band.read` |  | Get GradingBand. |
| 777 | `PATCH /v1/assessment/grading-bands/{gradingBandId}` | `GradingBandService/UpdateGradingBand` | `assessment.grading_band.update` |  | Update GradingBand. |
| 778 | `DELETE /v1/assessment/grading-bands/{gradingBandId}` | `GradingBandService/DeleteGradingBand` | `assessment.grading_band.delete` |  | Delete GradingBand. |
| 779 | `GET /v1/assessment/exam-types` | `ExamTypeService/ListExamTypes` | `assessment.exam_type.read` |  | List ExamTypes. |
| 780 | `POST /v1/assessment/exam-types` | `ExamTypeService/CreateExamType` | `assessment.exam_type.create` |  | Create ExamType. |
| 781 | `GET /v1/assessment/exam-types/{examTypeId}` | `ExamTypeService/GetExamType` | `assessment.exam_type.read` |  | Get ExamType. |
| 782 | `PATCH /v1/assessment/exam-types/{examTypeId}` | `ExamTypeService/UpdateExamType` | `assessment.exam_type.update` |  | Update ExamType. |
| 783 | `DELETE /v1/assessment/exam-types/{examTypeId}` | `ExamTypeService/DeleteExamType` | `assessment.exam_type.delete` |  | Delete ExamType. |
| 784 | `GET /v1/assessment/exam-sessions` | `ExamSessionService/ListExamSessions` | `assessment.exam_session.read` |  | List ExamSessions. |
| 785 | `POST /v1/assessment/exam-sessions` | `ExamSessionService/CreateExamSession` | `assessment.exam_session.create` |  | Create ExamSession. |
| 786 | `GET /v1/assessment/exam-sessions/{examSessionId}` | `ExamSessionService/GetExamSession` | `assessment.exam_session.read` |  | Get ExamSession. |
| 787 | `PATCH /v1/assessment/exam-sessions/{examSessionId}` | `ExamSessionService/UpdateExamSession` | `assessment.exam_session.update` |  | Update ExamSession. |
| 788 | `DELETE /v1/assessment/exam-sessions/{examSessionId}` | `ExamSessionService/DeleteExamSession` | `assessment.exam_session.delete` |  | Delete ExamSession. |
| 789 | `GET /v1/assessment/exams` | `ExamService/ListExams` | `assessment.exam.read` |  | List Exams. |
| 790 | `POST /v1/assessment/exams` | `ExamService/CreateExam` | `assessment.exam.create` |  | Create Exam. |
| 791 | `GET /v1/assessment/exams/{examId}` | `ExamService/GetExam` | `assessment.exam.read` |  | Get Exam. |
| 792 | `PATCH /v1/assessment/exams/{examId}` | `ExamService/UpdateExam` | `assessment.exam.update` |  | Update Exam. |
| 793 | `DELETE /v1/assessment/exams/{examId}` | `ExamService/DeleteExam` | `assessment.exam.delete` |  | Delete Exam. |
| 794 | `GET /v1/assessment/exams/{examId}/invigilators` | `ExamService/ListExamInvigilators` | `assessment.exam_invigilator.read` |  | List ExamInvigilators. |
| 795 | `POST /v1/assessment/exams/{examId}/invigilators` | `ExamService/CreateExamInvigilator` | `assessment.exam_invigilator.create` |  | Create ExamInvigilator. |
| 796 | `GET /v1/assessment/exams/{examId}/invigilators/{examInvigilatorId}` | `ExamService/GetExamInvigilator` | `assessment.exam_invigilator.read` |  | Get ExamInvigilator. |
| 797 | `PATCH /v1/assessment/exams/{examId}/invigilators/{examInvigilatorId}` | `ExamService/UpdateExamInvigilator` | `assessment.exam_invigilator.update` |  | Update ExamInvigilator. |
| 798 | `DELETE /v1/assessment/exams/{examId}/invigilators/{examInvigilatorId}` | `ExamService/DeleteExamInvigilator` | `assessment.exam_invigilator.delete` |  | Delete ExamInvigilator. |
| 799 | `GET /v1/assessment/exam-results` | `ExamResultService/ListExamResults` | `assessment.exam_result.read` |  | List ExamResults. |
| 800 | `GET /v1/assessment/exam-results/{examResultId}` | `ExamResultService/GetExamResult` | `assessment.exam_result.read` |  | Get ExamResult. |
| 801 | `PATCH /v1/assessment/exam-results/{examResultId}` | `ExamResultService/UpdateExamResult` | `assessment.exam_result.update` |  | Update ExamResult. |
| 802 | `GET /v1/assessment/result-templates` | `ResultTemplateService/ListResultTemplates` | `assessment.result_template.read` |  | List ResultTemplates. |
| 803 | `POST /v1/assessment/result-templates` | `ResultTemplateService/CreateResultTemplate` | `assessment.result_template.create` |  | Create ResultTemplate. |
| 804 | `GET /v1/assessment/result-templates/{resultTemplateId}` | `ResultTemplateService/GetResultTemplate` | `assessment.result_template.read` |  | Get ResultTemplate. |
| 805 | `PATCH /v1/assessment/result-templates/{resultTemplateId}` | `ResultTemplateService/UpdateResultTemplate` | `assessment.result_template.update` |  | Update ResultTemplate. |
| 806 | `DELETE /v1/assessment/result-templates/{resultTemplateId}` | `ResultTemplateService/DeleteResultTemplate` | `assessment.result_template.delete` |  | Delete ResultTemplate. |
| 807 | `GET /v1/assessment/result-templates/{resultTemplateId}/result-template-bands` | `ResultTemplateService/ListResultTemplateBands` | `assessment.result_template_band.read` |  | List ResultTemplateBands. |
| 808 | `POST /v1/assessment/result-templates/{resultTemplateId}/result-template-bands` | `ResultTemplateService/CreateResultTemplateBand` | `assessment.result_template_band.create` |  | Create ResultTemplateBand. |
| 809 | `GET /v1/assessment/result-templates/{resultTemplateId}/result-template-bands/{resultTemplateBandId}` | `ResultTemplateService/GetResultTemplateBand` | `assessment.result_template_band.read` |  | Get ResultTemplateBand. |
| 810 | `PATCH /v1/assessment/result-templates/{resultTemplateId}/result-template-bands/{resultTemplateBandId}` | `ResultTemplateService/UpdateResultTemplateBand` | `assessment.result_template_band.update` |  | Update ResultTemplateBand. |
| 811 | `DELETE /v1/assessment/result-templates/{resultTemplateId}/result-template-bands/{resultTemplateBandId}` | `ResultTemplateService/DeleteResultTemplateBand` | `assessment.result_template_band.delete` |  | Delete ResultTemplateBand. |
| 812 | `GET /v1/assessment/marksheets` | `MarksheetService/ListMarksheets` | `assessment.marksheet.read` |  | List Marksheets. |
| 813 | `POST /v1/assessment/marksheets` | `MarksheetService/CreateMarksheet` | `assessment.marksheet.create` |  | Create Marksheet. |
| 814 | `GET /v1/assessment/marksheets/{marksheetId}` | `MarksheetService/GetMarksheet` | `assessment.marksheet.read` |  | Get Marksheet. |
| 815 | `PATCH /v1/assessment/marksheets/{marksheetId}` | `MarksheetService/UpdateMarksheet` | `assessment.marksheet.update` |  | Update Marksheet. |
| 816 | `DELETE /v1/assessment/marksheets/{marksheetId}` | `MarksheetService/DeleteMarksheet` | `assessment.marksheet.delete` |  | Delete Marksheet. |
| 817 | `GET /v1/assessment/marksheet-lines` | `MarksheetLineService/ListMarksheetLines` | `assessment.marksheet_line.read` |  | List MarksheetLines. |
| 818 | `GET /v1/assessment/marksheet-lines/{marksheetLineId}` | `MarksheetLineService/GetMarksheetLine` | `assessment.marksheet_line.read` |  | Get MarksheetLine. |
| 819 | `POST /v1/assessment/homeworks/{homeworkId}:publish` | `HomeworkService/PublishHomework` | `assessment.homework.publish_homework` |  | Publish (or send for approval when approval is required). |
| 820 | `POST /v1/assessment/homeworks/{homeworkId}:approve` | `HomeworkService/ApproveHomework` | `assessment.homework.approve_homework` |  | Approve. |
| 821 | `POST /v1/assessment/homeworks/{homeworkId}:reject` | `HomeworkService/RejectHomework` | `assessment.homework.reject_homework` |  | Reject. |
| 822 | `POST /v1/assessment/homework-submissions/{homeworkSubmissionId}:check` | `HomeworkSubmissionService/CheckHomeworkSubmission` | `assessment.homework_submission.check_homework_submission` |  | Teacher marks the submission checked. |
| 823 | `POST /v1/assessment/homework-submissions/{homeworkSubmissionId}:reply` | `HomeworkSubmissionService/ReplyToHomeworkFeedback` | `assessment.homework_submission.reply_to_homework_feedback` |  | Student replies to comments. |
| 824 | `POST /v1/assessment/assignments/{assignmentId}:publish` | `AssignmentService/PublishAssignment` | `assessment.assignment.publish_assignment` |  | Publish (status published; becomes ongoing automatically at issued_on). |
| 825 | `POST /v1/assessment/assignments/{assignmentId}:approve` | `AssignmentService/ApproveAssignment` | `assessment.assignment.approve_assignment` |  | Approve. |
| 826 | `POST /v1/assessment/assignments/{assignmentId}:reject` | `AssignmentService/RejectAssignment` | `assessment.assignment.reject_assignment` |  | Reject. |
| 827 | `POST /v1/assessment/assignments/{assignmentId}:finish` | `AssignmentService/FinishAssignment` | `assessment.assignment.finish_assignment` |  | Close for submissions. |
| 828 | `POST /v1/assessment/assignments/{assignmentId}:cancel` | `AssignmentService/CancelAssignment` | `assessment.assignment.cancel_assignment` |  | Cancel. |
| 829 | `POST /v1/assessment/assignments/{assignmentId}:resetToDraft` | `AssignmentService/ResetAssignment` | `assessment.assignment.reset_assignment` |  | Back to draft. |
| 830 | `POST /v1/assessment/assignment-submissions/{assignmentSubmissionId}:submit` | `AssignmentSubmissionService/SubmitAssignmentSubmission` | `assessment.assignment_submission.submit_assignment_submission` |  | Student submits. |
| 831 | `POST /v1/assessment/assignment-submissions/{assignmentSubmissionId}:accept` | `AssignmentSubmissionService/AcceptAssignmentSubmission` | `assessment.assignment_submission.accept_assignment_submission` |  | Accept. |
| 832 | `POST /v1/assessment/assignment-submissions/{assignmentSubmissionId}:requestChanges` | `AssignmentSubmissionService/RequestSubmissionChanges` | `assessment.assignment_submission.request_submission_changes` |  | Ask for changes. |
| 833 | `POST /v1/assessment/assignment-submissions/{assignmentSubmissionId}:reject` | `AssignmentSubmissionService/RejectAssignmentSubmission` | `assessment.assignment_submission.reject_assignment_submission` |  | Reject. |
| 834 | `POST /v1/assessment/assignment-submissions/{assignmentSubmissionId}:resetToDraft` | `AssignmentSubmissionService/ResetAssignmentSubmission` | `assessment.assignment_submission.reset_assignment_submission` |  | Change and resubmit. |
| 835 | `POST /v1/assessment/assignment-submissions/{assignmentSubmissionId}:grade` | `AssignmentSubmissionService/GradeAssignmentSubmission` | `assessment.assignment_submission.grade_assignment_submission` |  | Award marks. |
| 836 | `POST /v1/assessment/exam-sessions/{examSessionId}:schedule` | `ExamSessionService/ScheduleExamSession` | `assessment.exam_session.schedule_exam_session` |  | Schedule. |
| 837 | `POST /v1/assessment/exam-sessions/{examSessionId}:markHeld` | `ExamSessionService/MarkExamSessionHeld` | `assessment.exam_session.mark_exam_session_held` |  | Mark held. |
| 838 | `POST /v1/assessment/exam-sessions/{examSessionId}:complete` | `ExamSessionService/CompleteExamSession` | `assessment.exam_session.complete_exam_session` |  | Done. |
| 839 | `POST /v1/assessment/exam-sessions/{examSessionId}:cancel` | `ExamSessionService/CancelExamSession` | `assessment.exam_session.cancel_exam_session` |  | Cancel. |
| 840 | `POST /v1/assessment/exam-sessions/{examSessionId}:resetToDraft` | `ExamSessionService/ResetExamSession` | `assessment.exam_session.reset_exam_session` |  | Back to draft. |
| 841 | `POST /v1/assessment/exams/{examId}:schedule` | `ExamService/ScheduleExam` | `assessment.exam.schedule_exam` |  | Schedule. |
| 842 | `POST /v1/assessment/exams/{examId}:markHeld` | `ExamService/MarkExamHeld` | `assessment.exam.mark_exam_held` |  | Mark held; attendees are registered. |
| 843 | `POST /v1/assessment/exams/{examId}:markResultsUpdated` | `ExamService/MarkExamResultsUpdated` | `assessment.exam.mark_exam_results_updated` |  | All marks entered. |
| 844 | `POST /v1/assessment/exams/{examId}:complete` | `ExamService/CompleteExam` | `assessment.exam.complete_exam` |  | Done. |
| 845 | `POST /v1/assessment/exams/{examId}:cancel` | `ExamService/CancelExam` | `assessment.exam.cancel_exam` |  | Cancel. |
| 846 | `POST /v1/assessment/exams/{examId}:resetToDraft` | `ExamService/ResetExam` | `assessment.exam.reset_exam` |  | Back to draft. |
| 847 | `POST /v1/assessment/exams/{examId}:populateResults` | `ExamService/PopulateExamResults` | `assessment.exam.populate_exam_results` |  | Create attendee rows from the students of the class / batch. |
| 848 | `POST /v1/assessment/exams/{examId}:allocateRooms` | `ExamService/AllocateExamRooms` | `assessment.exam.allocate_exam_rooms` |  | Distribute attendees across rooms by capacity. |
| 849 | `POST /v1/assessment/exams/{examId}:recordMarks` | `ExamService/RecordExamMarks` | `assessment.exam.record_exam_marks` |  | Enter or correct marks for many students. |
| 850 | `POST /v1/assessment/result-templates/{resultTemplateId}:generateResults` | `ResultTemplateService/GenerateResults` | `assessment.result_template.generate_results` |  | Compute result lines and the marksheet for the exam series. |
| 851 | `POST /v1/assessment/marksheets/{marksheetId}:validate` | `MarksheetService/ValidateMarksheet` | `assessment.marksheet.validate_marksheet` |  | Validate; results become visible. |
| 852 | `POST /v1/assessment/marksheets/{marksheetId}:cancel` | `MarksheetService/CancelMarksheet` | `assessment.marksheet.cancel_marksheet` |  | Cancel. |
| 853 | `POST /v1/assessment/marksheets/{marksheetId}:resetToDraft` | `MarksheetService/ResetMarksheet` | `assessment.marksheet.reset_marksheet` |  | Back to draft. |
| 854 | `GET /v1/assessment/students/{studentId}/results` | `MarksheetLineService/GetStudentResults` | `assessment.marksheet_line.get_student_results` |  | Report-card data for a student (validated marksheets only for students/guardians). |
| 855 | `POST /v1/assessment/marksheets/{marksheetId}:publish` | `MarksheetService/PublishMarksheet` | `assessment.marksheet.publish` |  | Release a validated marksheet to students. |
| 856 | `GET /v1/assessment/operations` | `OperationService/ListOperations` | `assessment.operation.read` |  | Long-running operations of this module started by the caller. |
| 857 | `GET /v1/assessment/operations/{operationId}` | `OperationService/GetOperation` | `assessment.operation.read` |  | Status of a long-running operation started in this module. |
| 858 | `POST /v1/assessment/operations/{operationId}:cancel` | `OperationService/CancelOperation` | `assessment.operation.cancel_operation` |  | Request cancellation of a running operation. |

## Learning Management (`lms`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 859 | `GET /v1/lms/categories` | `CategoryService/ListCategories` | `lms.category.read` | Public | List Categories. |
| 860 | `POST /v1/lms/categories` | `CategoryService/CreateCategory` | `lms.category.create` |  | Create Category. |
| 861 | `GET /v1/lms/categories/{categoryId}` | `CategoryService/GetCategory` | `lms.category.read` | Public | Get Category. |
| 862 | `PATCH /v1/lms/categories/{categoryId}` | `CategoryService/UpdateCategory` | `lms.category.update` |  | Update Category. |
| 863 | `DELETE /v1/lms/categories/{categoryId}` | `CategoryService/DeleteCategory` | `lms.category.delete` |  | Delete Category. |
| 864 | `GET /v1/lms/evaluators` | `EvaluatorService/ListEvaluators` | `lms.evaluator.read` |  | List Evaluators. |
| 865 | `POST /v1/lms/evaluators` | `EvaluatorService/CreateEvaluator` | `lms.evaluator.create` |  | Create Evaluator. |
| 866 | `GET /v1/lms/evaluators/{evaluatorId}` | `EvaluatorService/GetEvaluator` | `lms.evaluator.read` |  | Get Evaluator. |
| 867 | `PATCH /v1/lms/evaluators/{evaluatorId}` | `EvaluatorService/UpdateEvaluator` | `lms.evaluator.update` |  | Update Evaluator. |
| 868 | `DELETE /v1/lms/evaluators/{evaluatorId}` | `EvaluatorService/DeleteEvaluator` | `lms.evaluator.delete` |  | Delete Evaluator. |
| 869 | `GET /v1/lms/evaluators/{evaluatorId}/availability` | `EvaluatorService/ListEvaluatorAvailabilities` | `lms.evaluator_availability.read` |  | List EvaluatorAvailabilities. |
| 870 | `POST /v1/lms/evaluators/{evaluatorId}/availability` | `EvaluatorService/CreateEvaluatorAvailability` | `lms.evaluator_availability.create` |  | Create EvaluatorAvailability. |
| 871 | `GET /v1/lms/evaluators/{evaluatorId}/availability/{evaluatorAvailabilityId}` | `EvaluatorService/GetEvaluatorAvailability` | `lms.evaluator_availability.read` |  | Get EvaluatorAvailability. |
| 872 | `PATCH /v1/lms/evaluators/{evaluatorId}/availability/{evaluatorAvailabilityId}` | `EvaluatorService/UpdateEvaluatorAvailability` | `lms.evaluator_availability.update` |  | Update EvaluatorAvailability. |
| 873 | `DELETE /v1/lms/evaluators/{evaluatorId}/availability/{evaluatorAvailabilityId}` | `EvaluatorService/DeleteEvaluatorAvailability` | `lms.evaluator_availability.delete` |  | Delete EvaluatorAvailability. |
| 874 | `GET /v1/lms/courses` | `LmsCourseService/ListLmsCourses` | `lms.course.read` | Public | List LmsCourses. |
| 875 | `POST /v1/lms/courses` | `LmsCourseService/CreateLmsCourse` | `lms.course.create` |  | Create LmsCourse. |
| 876 | `GET /v1/lms/courses/{courseId}` | `LmsCourseService/GetLmsCourse` | `lms.course.read` | Public | Get LmsCourse. |
| 877 | `PATCH /v1/lms/courses/{courseId}` | `LmsCourseService/UpdateLmsCourse` | `lms.course.update` |  | Update LmsCourse. |
| 878 | `DELETE /v1/lms/courses/{courseId}` | `LmsCourseService/DeleteLmsCourse` | `lms.course.delete` |  | Delete LmsCourse. |
| 879 | `GET /v1/lms/courses/{courseId}/teachers` | `LmsCourseService/ListCourseTeachers` | `lms.course_teacher.read` |  | List CourseTeachers. |
| 880 | `POST /v1/lms/courses/{courseId}/teachers` | `LmsCourseService/CreateCourseTeacher` | `lms.course_teacher.create` |  | Create CourseTeacher. |
| 881 | `GET /v1/lms/courses/{courseId}/teachers/{courseTeacherId}` | `LmsCourseService/GetCourseTeacher` | `lms.course_teacher.read` |  | Get CourseTeacher. |
| 882 | `PATCH /v1/lms/courses/{courseId}/teachers/{courseTeacherId}` | `LmsCourseService/UpdateCourseTeacher` | `lms.course_teacher.update` |  | Update CourseTeacher. |
| 883 | `DELETE /v1/lms/courses/{courseId}/teachers/{courseTeacherId}` | `LmsCourseService/DeleteCourseTeacher` | `lms.course_teacher.delete` |  | Delete CourseTeacher. |
| 884 | `GET /v1/lms/courses/{courseId}/related-courses` | `LmsCourseService/ListRelatedCourses` | `lms.related_course.read` | Public | List RelatedCourses. |
| 885 | `POST /v1/lms/courses/{courseId}/related-courses` | `LmsCourseService/CreateRelatedCourse` | `lms.related_course.create` |  | Create RelatedCourse. |
| 886 | `GET /v1/lms/courses/{courseId}/related-courses/{relatedCourseId}` | `LmsCourseService/GetRelatedCourse` | `lms.related_course.read` |  | Get RelatedCourse. |
| 887 | `PATCH /v1/lms/courses/{courseId}/related-courses/{relatedCourseId}` | `LmsCourseService/UpdateRelatedCourse` | `lms.related_course.update` |  | Update RelatedCourse. |
| 888 | `DELETE /v1/lms/courses/{courseId}/related-courses/{relatedCourseId}` | `LmsCourseService/DeleteRelatedCourse` | `lms.related_course.delete` |  | Delete RelatedCourse. |
| 889 | `GET /v1/lms/courses/{courseId}/mentors` | `LmsCourseService/ListCourseMentors` | `lms.course_mentor.read` |  | List CourseMentors. |
| 890 | `POST /v1/lms/courses/{courseId}/mentors` | `LmsCourseService/CreateCourseMentor` | `lms.course_mentor.create` |  | Create CourseMentor. |
| 891 | `GET /v1/lms/courses/{courseId}/mentors/{courseMentorId}` | `LmsCourseService/GetCourseMentor` | `lms.course_mentor.read` |  | Get CourseMentor. |
| 892 | `PATCH /v1/lms/courses/{courseId}/mentors/{courseMentorId}` | `LmsCourseService/UpdateCourseMentor` | `lms.course_mentor.update` |  | Update CourseMentor. |
| 893 | `DELETE /v1/lms/courses/{courseId}/mentors/{courseMentorId}` | `LmsCourseService/DeleteCourseMentor` | `lms.course_mentor.delete` |  | Delete CourseMentor. |
| 894 | `GET /v1/lms/courses/{courseId}/chapters` | `LmsCourseService/ListChapters` | `lms.chapter.read` |  | List Chapters. |
| 895 | `POST /v1/lms/courses/{courseId}/chapters` | `LmsCourseService/CreateChapter` | `lms.chapter.create` |  | Create Chapter. |
| 896 | `GET /v1/lms/courses/{courseId}/chapters/{chapterId}` | `LmsCourseService/GetChapter` | `lms.chapter.read` |  | Get Chapter. |
| 897 | `PATCH /v1/lms/courses/{courseId}/chapters/{chapterId}` | `LmsCourseService/UpdateChapter` | `lms.chapter.update` |  | Update Chapter. |
| 898 | `DELETE /v1/lms/courses/{courseId}/chapters/{chapterId}` | `LmsCourseService/DeleteChapter` | `lms.chapter.delete` |  | Delete Chapter. |
| 899 | `GET /v1/lms/lessons` | `LessonService/ListLessons` | `lms.lesson.read` |  | List Lessons. |
| 900 | `POST /v1/lms/lessons` | `LessonService/CreateLesson` | `lms.lesson.create` |  | Create Lesson. |
| 901 | `GET /v1/lms/lessons/{lessonId}` | `LessonService/GetLesson` | `lms.lesson.read` |  | Get Lesson. |
| 902 | `PATCH /v1/lms/lessons/{lessonId}` | `LessonService/UpdateLesson` | `lms.lesson.update` |  | Update Lesson. |
| 903 | `DELETE /v1/lms/lessons/{lessonId}` | `LessonService/DeleteLesson` | `lms.lesson.delete` |  | Delete Lesson. |
| 904 | `GET /v1/lms/quizzes` | `QuizService/ListQuizzes` | `lms.quiz.read` |  | List Quizzes. |
| 905 | `POST /v1/lms/quizzes` | `QuizService/CreateQuiz` | `lms.quiz.create` |  | Create Quiz. |
| 906 | `GET /v1/lms/quizzes/{quizId}` | `QuizService/GetQuiz` | `lms.quiz.read` |  | Get Quiz. |
| 907 | `PATCH /v1/lms/quizzes/{quizId}` | `QuizService/UpdateQuiz` | `lms.quiz.update` |  | Update Quiz. |
| 908 | `DELETE /v1/lms/quizzes/{quizId}` | `QuizService/DeleteQuiz` | `lms.quiz.delete` |  | Delete Quiz. |
| 909 | `GET /v1/lms/questions` | `QuestionService/ListQuestions` | `lms.question.read` |  | List Questions. |
| 910 | `POST /v1/lms/questions` | `QuestionService/CreateQuestion` | `lms.question.create` |  | Create Question. |
| 911 | `GET /v1/lms/questions/{questionId}` | `QuestionService/GetQuestion` | `lms.question.read` |  | Get Question. |
| 912 | `PATCH /v1/lms/questions/{questionId}` | `QuestionService/UpdateQuestion` | `lms.question.update` |  | Update Question. |
| 913 | `DELETE /v1/lms/questions/{questionId}` | `QuestionService/DeleteQuestion` | `lms.question.delete` |  | Delete Question. |
| 914 | `GET /v1/lms/questions/{questionId}/options` | `QuestionService/ListQuestionOptions` | `lms.question_option.read` |  | List QuestionOptions. |
| 915 | `POST /v1/lms/questions/{questionId}/options` | `QuestionService/CreateQuestionOption` | `lms.question_option.create` |  | Create QuestionOption. |
| 916 | `GET /v1/lms/questions/{questionId}/options/{questionOptionId}` | `QuestionService/GetQuestionOption` | `lms.question_option.read` |  | Get QuestionOption. |
| 917 | `PATCH /v1/lms/questions/{questionId}/options/{questionOptionId}` | `QuestionService/UpdateQuestionOption` | `lms.question_option.update` |  | Update QuestionOption. |
| 918 | `DELETE /v1/lms/questions/{questionId}/options/{questionOptionId}` | `QuestionService/DeleteQuestionOption` | `lms.question_option.delete` |  | Delete QuestionOption. |
| 919 | `GET /v1/lms/questions/{questionId}/accepted-answers` | `QuestionService/ListQuestionAcceptedAnswers` | `lms.question_accepted_answer.read` |  | List QuestionAcceptedAnswers. |
| 920 | `POST /v1/lms/questions/{questionId}/accepted-answers` | `QuestionService/CreateQuestionAcceptedAnswer` | `lms.question_accepted_answer.create` |  | Create QuestionAcceptedAnswer. |
| 921 | `GET /v1/lms/questions/{questionId}/accepted-answers/{questionAcceptedAnswerId}` | `QuestionService/GetQuestionAcceptedAnswer` | `lms.question_accepted_answer.read` |  | Get QuestionAcceptedAnswer. |
| 922 | `PATCH /v1/lms/questions/{questionId}/accepted-answers/{questionAcceptedAnswerId}` | `QuestionService/UpdateQuestionAcceptedAnswer` | `lms.question_accepted_answer.update` |  | Update QuestionAcceptedAnswer. |
| 923 | `DELETE /v1/lms/questions/{questionId}/accepted-answers/{questionAcceptedAnswerId}` | `QuestionService/DeleteQuestionAcceptedAnswer` | `lms.question_accepted_answer.delete` |  | Delete QuestionAcceptedAnswer. |
| 924 | `GET /v1/lms/quizzes/{quizId}/questions` | `QuizService/ListQuizQuestions` | `lms.quiz_question.read` |  | List QuizQuestions. |
| 925 | `POST /v1/lms/quizzes/{quizId}/questions` | `QuizService/CreateQuizQuestion` | `lms.quiz_question.create` |  | Create QuizQuestion. |
| 926 | `GET /v1/lms/quizzes/{quizId}/questions/{quizQuestionId}` | `QuizService/GetQuizQuestion` | `lms.quiz_question.read` |  | Get QuizQuestion. |
| 927 | `PATCH /v1/lms/quizzes/{quizId}/questions/{quizQuestionId}` | `QuizService/UpdateQuizQuestion` | `lms.quiz_question.update` |  | Update QuizQuestion. |
| 928 | `DELETE /v1/lms/quizzes/{quizId}/questions/{quizQuestionId}` | `QuizService/DeleteQuizQuestion` | `lms.quiz_question.delete` |  | Delete QuizQuestion. |
| 929 | `GET /v1/lms/quiz-submissions` | `QuizSubmissionService/ListQuizSubmissions` | `lms.quiz_submission.read` |  | List QuizSubmissions. |
| 930 | `GET /v1/lms/quiz-submissions/{quizSubmissionId}` | `QuizSubmissionService/GetQuizSubmission` | `lms.quiz_submission.read` |  | Get QuizSubmission. |
| 931 | `GET /v1/lms/quiz-submissions/{quizSubmissionId}/answers` | `QuizSubmissionService/ListQuizAnswers` | `lms.quiz_answer.read` |  | List QuizAnswers. |
| 932 | `GET /v1/lms/quiz-submissions/{quizSubmissionId}/answers/{quizAnswerId}` | `QuizSubmissionService/GetQuizAnswer` | `lms.quiz_answer.read` |  | Get QuizAnswer. |
| 933 | `GET /v1/lms/quiz-submissions/{quizSubmissionId}/violations` | `QuizSubmissionService/ListQuizViolations` | `lms.quiz_violation.read` |  | List QuizViolations. |
| 934 | `GET /v1/lms/quiz-submissions/{quizSubmissionId}/violations/{quizViolationId}` | `QuizSubmissionService/GetQuizViolation` | `lms.quiz_violation.read` |  | Get QuizViolation. |
| 935 | `GET /v1/lms/assignments` | `LmsAssignmentService/ListLmsAssignments` | `lms.assignment.read` |  | List LmsAssignments. |
| 936 | `POST /v1/lms/assignments` | `LmsAssignmentService/CreateLmsAssignment` | `lms.assignment.create` |  | Create LmsAssignment. |
| 937 | `GET /v1/lms/assignments/{assignmentId}` | `LmsAssignmentService/GetLmsAssignment` | `lms.assignment.read` |  | Get LmsAssignment. |
| 938 | `PATCH /v1/lms/assignments/{assignmentId}` | `LmsAssignmentService/UpdateLmsAssignment` | `lms.assignment.update` |  | Update LmsAssignment. |
| 939 | `DELETE /v1/lms/assignments/{assignmentId}` | `LmsAssignmentService/DeleteLmsAssignment` | `lms.assignment.delete` |  | Delete LmsAssignment. |
| 940 | `GET /v1/lms/assignment-submissions` | `LmsAssignmentSubmissionService/ListLmsAssignmentSubmissions` | `lms.assignment_submission.read` |  | List LmsAssignmentSubmissions. |
| 941 | `POST /v1/lms/assignment-submissions` | `LmsAssignmentSubmissionService/CreateLmsAssignmentSubmission` | `lms.assignment_submission.create` |  | Create LmsAssignmentSubmission. |
| 942 | `GET /v1/lms/assignment-submissions/{assignmentSubmissionId}` | `LmsAssignmentSubmissionService/GetLmsAssignmentSubmission` | `lms.assignment_submission.read` |  | Get LmsAssignmentSubmission. |
| 943 | `PATCH /v1/lms/assignment-submissions/{assignmentSubmissionId}` | `LmsAssignmentSubmissionService/UpdateLmsAssignmentSubmission` | `lms.assignment_submission.update` |  | Update LmsAssignmentSubmission. |
| 944 | `DELETE /v1/lms/assignment-submissions/{assignmentSubmissionId}` | `LmsAssignmentSubmissionService/DeleteLmsAssignmentSubmission` | `lms.assignment_submission.delete` |  | Delete LmsAssignmentSubmission. |
| 945 | `GET /v1/lms/programming-exercises` | `ProgrammingExerciseService/ListProgrammingExercises` | `lms.programming_exercise.read` |  | List ProgrammingExercises. |
| 946 | `POST /v1/lms/programming-exercises` | `ProgrammingExerciseService/CreateProgrammingExercise` | `lms.programming_exercise.create` |  | Create ProgrammingExercise. |
| 947 | `GET /v1/lms/programming-exercises/{programmingExerciseId}` | `ProgrammingExerciseService/GetProgrammingExercise` | `lms.programming_exercise.read` |  | Get ProgrammingExercise. |
| 948 | `PATCH /v1/lms/programming-exercises/{programmingExerciseId}` | `ProgrammingExerciseService/UpdateProgrammingExercise` | `lms.programming_exercise.update` |  | Update ProgrammingExercise. |
| 949 | `DELETE /v1/lms/programming-exercises/{programmingExerciseId}` | `ProgrammingExerciseService/DeleteProgrammingExercise` | `lms.programming_exercise.delete` |  | Delete ProgrammingExercise. |
| 950 | `GET /v1/lms/programming-exercises/{programmingExerciseId}/test-cases` | `ProgrammingExerciseService/ListExerciseTestCases` | `lms.exercise_test_case.read` |  | List ExerciseTestCases. |
| 951 | `POST /v1/lms/programming-exercises/{programmingExerciseId}/test-cases` | `ProgrammingExerciseService/CreateExerciseTestCase` | `lms.exercise_test_case.create` |  | Create ExerciseTestCase. |
| 952 | `GET /v1/lms/programming-exercises/{programmingExerciseId}/test-cases/{exerciseTestCaseId}` | `ProgrammingExerciseService/GetExerciseTestCase` | `lms.exercise_test_case.read` |  | Get ExerciseTestCase. |
| 953 | `PATCH /v1/lms/programming-exercises/{programmingExerciseId}/test-cases/{exerciseTestCaseId}` | `ProgrammingExerciseService/UpdateExerciseTestCase` | `lms.exercise_test_case.update` |  | Update ExerciseTestCase. |
| 954 | `DELETE /v1/lms/programming-exercises/{programmingExerciseId}/test-cases/{exerciseTestCaseId}` | `ProgrammingExerciseService/DeleteExerciseTestCase` | `lms.exercise_test_case.delete` |  | Delete ExerciseTestCase. |
| 955 | `GET /v1/lms/exercise-submissions` | `ExerciseSubmissionService/ListExerciseSubmissions` | `lms.exercise_submission.read` |  | List ExerciseSubmissions. |
| 956 | `GET /v1/lms/exercise-submissions/{exerciseSubmissionId}` | `ExerciseSubmissionService/GetExerciseSubmission` | `lms.exercise_submission.read` |  | Get ExerciseSubmission. |
| 957 | `GET /v1/lms/exercise-submissions/{exerciseSubmissionId}/test-results` | `ExerciseSubmissionService/ListExerciseTestResults` | `lms.exercise_test_result.read` |  | List ExerciseTestResults. |
| 958 | `GET /v1/lms/exercise-submissions/{exerciseSubmissionId}/test-results/{exerciseTestResultId}` | `ExerciseSubmissionService/GetExerciseTestResult` | `lms.exercise_test_result.read` |  | Get ExerciseTestResult. |
| 959 | `GET /v1/lms/content-authors` | `ContentAuthorService/ListContentAuthors` | `lms.content_author.read` |  | List ContentAuthors. |
| 960 | `POST /v1/lms/content-authors` | `ContentAuthorService/CreateContentAuthor` | `lms.content_author.create` |  | Create ContentAuthor. |
| 961 | `GET /v1/lms/content-authors/{contentAuthorId}` | `ContentAuthorService/GetContentAuthor` | `lms.content_author.read` |  | Get ContentAuthor. |
| 962 | `PATCH /v1/lms/content-authors/{contentAuthorId}` | `ContentAuthorService/UpdateContentAuthor` | `lms.content_author.update` |  | Update ContentAuthor. |
| 963 | `DELETE /v1/lms/content-authors/{contentAuthorId}` | `ContentAuthorService/DeleteContentAuthor` | `lms.content_author.delete` |  | Delete ContentAuthor. |
| 964 | `GET /v1/lms/lessons/{lessonId}/activities` | `LessonService/ListLessonActivities` | `lms.lesson_activity.read` |  | List LessonActivities. |
| 965 | `POST /v1/lms/lessons/{lessonId}/activities` | `LessonService/CreateLessonActivity` | `lms.lesson_activity.create` |  | Create LessonActivity. |
| 966 | `GET /v1/lms/lessons/{lessonId}/activities/{lessonActivityId}` | `LessonService/GetLessonActivity` | `lms.lesson_activity.read` |  | Get LessonActivity. |
| 967 | `PATCH /v1/lms/lessons/{lessonId}/activities/{lessonActivityId}` | `LessonService/UpdateLessonActivity` | `lms.lesson_activity.update` |  | Update LessonActivity. |
| 968 | `DELETE /v1/lms/lessons/{lessonId}/activities/{lessonActivityId}` | `LessonService/DeleteLessonActivity` | `lms.lesson_activity.delete` |  | Delete LessonActivity. |
| 969 | `GET /v1/lms/enrollments` | `EnrollmentService/ListEnrollments` | `lms.enrollment.read` |  | List Enrollments. |
| 970 | `GET /v1/lms/enrollments/{enrollmentId}` | `EnrollmentService/GetEnrollment` | `lms.enrollment.read` |  | Get Enrollment. |
| 971 | `PATCH /v1/lms/enrollments/{enrollmentId}` | `EnrollmentService/UpdateEnrollment` | `lms.enrollment.update` |  | Update Enrollment. |
| 972 | `DELETE /v1/lms/enrollments/{enrollmentId}` | `EnrollmentService/DeleteEnrollment` | `lms.enrollment.delete` |  | Delete Enrollment. |
| 973 | `GET /v1/lms/lesson-progress` | `LessonProgressService/ListLessonProgress` | `lms.lesson_progress.read` |  | List LessonProgress. |
| 974 | `GET /v1/lms/lesson-progress/{lessonProgressId}` | `LessonProgressService/GetLessonProgress` | `lms.lesson_progress.read` |  | Get LessonProgress. |
| 975 | `GET /v1/lms/lessons/{lessonId}/notes` | `LessonService/ListLessonNotes` | `lms.lesson_note.read` |  | List LessonNotes. |
| 976 | `POST /v1/lms/lessons/{lessonId}/notes` | `LessonService/CreateLessonNote` | `lms.lesson_note.create` |  | Create LessonNote. |
| 977 | `GET /v1/lms/lessons/{lessonId}/notes/{lessonNoteId}` | `LessonService/GetLessonNote` | `lms.lesson_note.read` |  | Get LessonNote. |
| 978 | `PATCH /v1/lms/lessons/{lessonId}/notes/{lessonNoteId}` | `LessonService/UpdateLessonNote` | `lms.lesson_note.update` |  | Update LessonNote. |
| 979 | `DELETE /v1/lms/lessons/{lessonId}/notes/{lessonNoteId}` | `LessonService/DeleteLessonNote` | `lms.lesson_note.delete` |  | Delete LessonNote. |
| 980 | `GET /v1/lms/lessons/{lessonId}/video-watches` | `LessonService/ListVideoWatches` | `lms.video_watch.read` |  | List VideoWatches. |
| 981 | `GET /v1/lms/lessons/{lessonId}/video-watches/{videoWatchId}` | `LessonService/GetVideoWatch` | `lms.video_watch.read` |  | Get VideoWatch. |
| 982 | `GET /v1/lms/courses/{courseId}/reviews` | `LmsCourseService/ListCourseReviews` | `lms.course_review.read` | Public | List CourseReviews. |
| 983 | `POST /v1/lms/courses/{courseId}/reviews` | `LmsCourseService/CreateCourseReview` | `lms.course_review.create` |  | Create CourseReview. |
| 984 | `GET /v1/lms/courses/{courseId}/reviews/{courseReviewId}` | `LmsCourseService/GetCourseReview` | `lms.course_review.read` |  | Get CourseReview. |
| 985 | `PATCH /v1/lms/courses/{courseId}/reviews/{courseReviewId}` | `LmsCourseService/UpdateCourseReview` | `lms.course_review.update` |  | Update CourseReview. |
| 986 | `DELETE /v1/lms/courses/{courseId}/reviews/{courseReviewId}` | `LmsCourseService/DeleteCourseReview` | `lms.course_review.delete` |  | Delete CourseReview. |
| 987 | `GET /v1/lms/courses/{courseId}/interests` | `LmsCourseService/ListCourseInterests` | `lms.course_interest.read` |  | List CourseInterests. |
| 988 | `POST /v1/lms/courses/{courseId}/interests` | `LmsCourseService/CreateCourseInterest` | `lms.course_interest.create` |  | Create CourseInterest. |
| 989 | `GET /v1/lms/courses/{courseId}/interests/{courseInterestId}` | `LmsCourseService/GetCourseInterest` | `lms.course_interest.read` |  | Get CourseInterest. |
| 990 | `PATCH /v1/lms/courses/{courseId}/interests/{courseInterestId}` | `LmsCourseService/UpdateCourseInterest` | `lms.course_interest.update` |  | Update CourseInterest. |
| 991 | `DELETE /v1/lms/courses/{courseId}/interests/{courseInterestId}` | `LmsCourseService/DeleteCourseInterest` | `lms.course_interest.delete` |  | Delete CourseInterest. |
| 992 | `GET /v1/lms/conferencing-accounts` | `ConferencingAccountService/ListConferencingAccounts` | `lms.conferencing_account.read` |  | List ConferencingAccounts. |
| 993 | `POST /v1/lms/conferencing-accounts` | `ConferencingAccountService/CreateConferencingAccount` | `lms.conferencing_account.create` |  | Create ConferencingAccount. |
| 994 | `GET /v1/lms/conferencing-accounts/{conferencingAccountId}` | `ConferencingAccountService/GetConferencingAccount` | `lms.conferencing_account.read` |  | Get ConferencingAccount. |
| 995 | `PATCH /v1/lms/conferencing-accounts/{conferencingAccountId}` | `ConferencingAccountService/UpdateConferencingAccount` | `lms.conferencing_account.update` |  | Update ConferencingAccount. |
| 996 | `DELETE /v1/lms/conferencing-accounts/{conferencingAccountId}` | `ConferencingAccountService/DeleteConferencingAccount` | `lms.conferencing_account.delete` |  | Delete ConferencingAccount. |
| 997 | `GET /v1/lms/timetable-templates` | `TimetableTemplateService/ListTimetableTemplates` | `lms.timetable_template.read` |  | List TimetableTemplates. |
| 998 | `POST /v1/lms/timetable-templates` | `TimetableTemplateService/CreateTimetableTemplate` | `lms.timetable_template.create` |  | Create TimetableTemplate. |
| 999 | `GET /v1/lms/timetable-templates/{timetableTemplateId}` | `TimetableTemplateService/GetTimetableTemplate` | `lms.timetable_template.read` |  | Get TimetableTemplate. |
| 1000 | `PATCH /v1/lms/timetable-templates/{timetableTemplateId}` | `TimetableTemplateService/UpdateTimetableTemplate` | `lms.timetable_template.update` |  | Update TimetableTemplate. |
| 1001 | `DELETE /v1/lms/timetable-templates/{timetableTemplateId}` | `TimetableTemplateService/DeleteTimetableTemplate` | `lms.timetable_template.delete` |  | Delete TimetableTemplate. |
| 1002 | `GET /v1/lms/cohorts` | `CohortService/ListCohorts` | `lms.cohort.read` | Public | List Cohorts. |
| 1003 | `POST /v1/lms/cohorts` | `CohortService/CreateCohort` | `lms.cohort.create` |  | Create Cohort. |
| 1004 | `GET /v1/lms/cohorts/{cohortId}` | `CohortService/GetCohort` | `lms.cohort.read` | Public | Get Cohort. |
| 1005 | `PATCH /v1/lms/cohorts/{cohortId}` | `CohortService/UpdateCohort` | `lms.cohort.update` |  | Update Cohort. |
| 1006 | `DELETE /v1/lms/cohorts/{cohortId}` | `CohortService/DeleteCohort` | `lms.cohort.delete` |  | Delete Cohort. |
| 1007 | `GET /v1/lms/cohorts/{cohortId}/courses` | `CohortService/ListCohortCourses` | `lms.cohort_course.read` | Public | List CohortCourses. |
| 1008 | `POST /v1/lms/cohorts/{cohortId}/courses` | `CohortService/CreateCohortCourse` | `lms.cohort_course.create` |  | Create CohortCourse. |
| 1009 | `GET /v1/lms/cohorts/{cohortId}/courses/{cohortCourseId}` | `CohortService/GetCohortCourse` | `lms.cohort_course.read` |  | Get CohortCourse. |
| 1010 | `PATCH /v1/lms/cohorts/{cohortId}/courses/{cohortCourseId}` | `CohortService/UpdateCohortCourse` | `lms.cohort_course.update` |  | Update CohortCourse. |
| 1011 | `DELETE /v1/lms/cohorts/{cohortId}/courses/{cohortCourseId}` | `CohortService/DeleteCohortCourse` | `lms.cohort_course.delete` |  | Delete CohortCourse. |
| 1012 | `GET /v1/lms/cohorts/{cohortId}/teachers` | `CohortService/ListCohortTeachers` | `lms.cohort_teacher.read` |  | List CohortTeachers. |
| 1013 | `POST /v1/lms/cohorts/{cohortId}/teachers` | `CohortService/CreateCohortTeacher` | `lms.cohort_teacher.create` |  | Create CohortTeacher. |
| 1014 | `GET /v1/lms/cohorts/{cohortId}/teachers/{cohortTeacherId}` | `CohortService/GetCohortTeacher` | `lms.cohort_teacher.read` |  | Get CohortTeacher. |
| 1015 | `PATCH /v1/lms/cohorts/{cohortId}/teachers/{cohortTeacherId}` | `CohortService/UpdateCohortTeacher` | `lms.cohort_teacher.update` |  | Update CohortTeacher. |
| 1016 | `DELETE /v1/lms/cohorts/{cohortId}/teachers/{cohortTeacherId}` | `CohortService/DeleteCohortTeacher` | `lms.cohort_teacher.delete` |  | Delete CohortTeacher. |
| 1017 | `GET /v1/lms/cohorts/{cohortId}/activities` | `CohortService/ListCohortActivities` | `lms.cohort_activity.read` |  | List CohortActivities. |
| 1018 | `POST /v1/lms/cohorts/{cohortId}/activities` | `CohortService/CreateCohortActivity` | `lms.cohort_activity.create` |  | Create CohortActivity. |
| 1019 | `GET /v1/lms/cohorts/{cohortId}/activities/{cohortActivityId}` | `CohortService/GetCohortActivity` | `lms.cohort_activity.read` |  | Get CohortActivity. |
| 1020 | `PATCH /v1/lms/cohorts/{cohortId}/activities/{cohortActivityId}` | `CohortService/UpdateCohortActivity` | `lms.cohort_activity.update` |  | Update CohortActivity. |
| 1021 | `DELETE /v1/lms/cohorts/{cohortId}/activities/{cohortActivityId}` | `CohortService/DeleteCohortActivity` | `lms.cohort_activity.delete` |  | Delete CohortActivity. |
| 1022 | `GET /v1/lms/timetable-entries` | `TimetableEntryService/ListTimetableEntries` | `lms.timetable_entry.read` |  | List TimetableEntries. |
| 1023 | `POST /v1/lms/timetable-entries` | `TimetableEntryService/CreateTimetableEntry` | `lms.timetable_entry.create` |  | Create TimetableEntry. |
| 1024 | `GET /v1/lms/timetable-entries/{timetableEntryId}` | `TimetableEntryService/GetTimetableEntry` | `lms.timetable_entry.read` |  | Get TimetableEntry. |
| 1025 | `PATCH /v1/lms/timetable-entries/{timetableEntryId}` | `TimetableEntryService/UpdateTimetableEntry` | `lms.timetable_entry.update` |  | Update TimetableEntry. |
| 1026 | `DELETE /v1/lms/timetable-entries/{timetableEntryId}` | `TimetableEntryService/DeleteTimetableEntry` | `lms.timetable_entry.delete` |  | Delete TimetableEntry. |
| 1027 | `GET /v1/lms/timetable-legends` | `TimetableLegendService/ListTimetableLegends` | `lms.timetable_legend.read` |  | List TimetableLegends. |
| 1028 | `POST /v1/lms/timetable-legends` | `TimetableLegendService/CreateTimetableLegend` | `lms.timetable_legend.create` |  | Create TimetableLegend. |
| 1029 | `GET /v1/lms/timetable-legends/{timetableLegendId}` | `TimetableLegendService/GetTimetableLegend` | `lms.timetable_legend.read` |  | Get TimetableLegend. |
| 1030 | `PATCH /v1/lms/timetable-legends/{timetableLegendId}` | `TimetableLegendService/UpdateTimetableLegend` | `lms.timetable_legend.update` |  | Update TimetableLegend. |
| 1031 | `DELETE /v1/lms/timetable-legends/{timetableLegendId}` | `TimetableLegendService/DeleteTimetableLegend` | `lms.timetable_legend.delete` |  | Delete TimetableLegend. |
| 1032 | `GET /v1/lms/cohort-enrollments` | `CohortEnrollmentService/ListCohortEnrollments` | `lms.cohort_enrollment.read` |  | List CohortEnrollments. |
| 1033 | `GET /v1/lms/cohort-enrollments/{cohortEnrollmentId}` | `CohortEnrollmentService/GetCohortEnrollment` | `lms.cohort_enrollment.read` |  | Get CohortEnrollment. |
| 1034 | `PATCH /v1/lms/cohort-enrollments/{cohortEnrollmentId}` | `CohortEnrollmentService/UpdateCohortEnrollment` | `lms.cohort_enrollment.update` |  | Update CohortEnrollment. |
| 1035 | `DELETE /v1/lms/cohort-enrollments/{cohortEnrollmentId}` | `CohortEnrollmentService/DeleteCohortEnrollment` | `lms.cohort_enrollment.delete` |  | Delete CohortEnrollment. |
| 1036 | `GET /v1/lms/cohorts/{cohortId}/feedback` | `CohortService/ListCohortFeedback` | `lms.cohort_feedback.read` |  | List CohortFeedback. |
| 1037 | `POST /v1/lms/cohorts/{cohortId}/feedback` | `CohortService/CreateCohortFeedback` | `lms.cohort_feedback.create` |  | Create CohortFeedback. |
| 1038 | `GET /v1/lms/cohorts/{cohortId}/feedback/{cohortFeedbackId}` | `CohortService/GetCohortFeedback` | `lms.cohort_feedback.read` |  | Get CohortFeedback. |
| 1039 | `PATCH /v1/lms/cohorts/{cohortId}/feedback/{cohortFeedbackId}` | `CohortService/UpdateCohortFeedback` | `lms.cohort_feedback.update` |  | Update CohortFeedback. |
| 1040 | `DELETE /v1/lms/cohorts/{cohortId}/feedback/{cohortFeedbackId}` | `CohortService/DeleteCohortFeedback` | `lms.cohort_feedback.delete` |  | Delete CohortFeedback. |
| 1041 | `GET /v1/lms/live-classes` | `LiveClassService/ListLiveClasses` | `lms.live_class.read` |  | List LiveClasses. |
| 1042 | `POST /v1/lms/live-classes` | `LiveClassService/CreateLiveClass` | `lms.live_class.create` |  | Create LiveClass. |
| 1043 | `GET /v1/lms/live-classes/{liveClassId}` | `LiveClassService/GetLiveClass` | `lms.live_class.read` |  | Get LiveClass. |
| 1044 | `PATCH /v1/lms/live-classes/{liveClassId}` | `LiveClassService/UpdateLiveClass` | `lms.live_class.update` |  | Update LiveClass. |
| 1045 | `DELETE /v1/lms/live-classes/{liveClassId}` | `LiveClassService/DeleteLiveClass` | `lms.live_class.delete` |  | Delete LiveClass. |
| 1046 | `GET /v1/lms/live-classes/{liveClassId}/participants` | `LiveClassService/ListLiveClassParticipants` | `lms.live_class_participant.read` |  | List LiveClassParticipants. |
| 1047 | `GET /v1/lms/live-classes/{liveClassId}/participants/{liveClassParticipantId}` | `LiveClassService/GetLiveClassParticipant` | `lms.live_class_participant.read` |  | Get LiveClassParticipant. |
| 1048 | `GET /v1/lms/learning-programs` | `LearningProgramService/ListLearningPrograms` | `lms.learning_program.read` | Public | List LearningPrograms. |
| 1049 | `POST /v1/lms/learning-programs` | `LearningProgramService/CreateLearningProgram` | `lms.learning_program.create` |  | Create LearningProgram. |
| 1050 | `GET /v1/lms/learning-programs/{learningProgramId}` | `LearningProgramService/GetLearningProgram` | `lms.learning_program.read` | Public | Get LearningProgram. |
| 1051 | `PATCH /v1/lms/learning-programs/{learningProgramId}` | `LearningProgramService/UpdateLearningProgram` | `lms.learning_program.update` |  | Update LearningProgram. |
| 1052 | `DELETE /v1/lms/learning-programs/{learningProgramId}` | `LearningProgramService/DeleteLearningProgram` | `lms.learning_program.delete` |  | Delete LearningProgram. |
| 1053 | `GET /v1/lms/learning-programs/{learningProgramId}/courses` | `LearningProgramService/ListLearningProgramCourses` | `lms.learning_program_course.read` |  | List LearningProgramCourses. |
| 1054 | `POST /v1/lms/learning-programs/{learningProgramId}/courses` | `LearningProgramService/CreateLearningProgramCourse` | `lms.learning_program_course.create` |  | Create LearningProgramCourse. |
| 1055 | `GET /v1/lms/learning-programs/{learningProgramId}/courses/{learningProgramCourseId}` | `LearningProgramService/GetLearningProgramCourse` | `lms.learning_program_course.read` |  | Get LearningProgramCourse. |
| 1056 | `PATCH /v1/lms/learning-programs/{learningProgramId}/courses/{learningProgramCourseId}` | `LearningProgramService/UpdateLearningProgramCourse` | `lms.learning_program_course.update` |  | Update LearningProgramCourse. |
| 1057 | `DELETE /v1/lms/learning-programs/{learningProgramId}/courses/{learningProgramCourseId}` | `LearningProgramService/DeleteLearningProgramCourse` | `lms.learning_program_course.delete` |  | Delete LearningProgramCourse. |
| 1058 | `GET /v1/lms/learning-programs/{learningProgramId}/members` | `LearningProgramService/ListLearningProgramMembers` | `lms.learning_program_member.read` |  | List LearningProgramMembers. |
| 1059 | `POST /v1/lms/learning-programs/{learningProgramId}/members` | `LearningProgramService/CreateLearningProgramMember` | `lms.learning_program_member.create` |  | Create LearningProgramMember. |
| 1060 | `GET /v1/lms/learning-programs/{learningProgramId}/members/{learningProgramMemberId}` | `LearningProgramService/GetLearningProgramMember` | `lms.learning_program_member.read` |  | Get LearningProgramMember. |
| 1061 | `PATCH /v1/lms/learning-programs/{learningProgramId}/members/{learningProgramMemberId}` | `LearningProgramService/UpdateLearningProgramMember` | `lms.learning_program_member.update` |  | Update LearningProgramMember. |
| 1062 | `DELETE /v1/lms/learning-programs/{learningProgramId}/members/{learningProgramMemberId}` | `LearningProgramService/DeleteLearningProgramMember` | `lms.learning_program_member.delete` |  | Delete LearningProgramMember. |
| 1063 | `GET /v1/lms/certificates` | `CertificateService/ListCertificates` | `lms.certificate.read` |  | List Certificates. |
| 1064 | `GET /v1/lms/certificates/{certificateId}` | `CertificateService/GetCertificate` | `lms.certificate.read` |  | Get Certificate. |
| 1065 | `GET /v1/lms/certificate-requests` | `CertificateRequestService/ListCertificateRequests` | `lms.certificate_request.read` |  | List CertificateRequests. |
| 1066 | `POST /v1/lms/certificate-requests` | `CertificateRequestService/CreateCertificateRequest` | `lms.certificate_request.create` |  | Create CertificateRequest. |
| 1067 | `GET /v1/lms/certificate-requests/{certificateRequestId}` | `CertificateRequestService/GetCertificateRequest` | `lms.certificate_request.read` |  | Get CertificateRequest. |
| 1068 | `PATCH /v1/lms/certificate-requests/{certificateRequestId}` | `CertificateRequestService/UpdateCertificateRequest` | `lms.certificate_request.update` |  | Update CertificateRequest. |
| 1069 | `DELETE /v1/lms/certificate-requests/{certificateRequestId}` | `CertificateRequestService/DeleteCertificateRequest` | `lms.certificate_request.delete` |  | Delete CertificateRequest. |
| 1070 | `GET /v1/lms/certificate-evaluations` | `CertificateEvaluationService/ListCertificateEvaluations` | `lms.certificate_evaluation.read` |  | List CertificateEvaluations. |
| 1071 | `POST /v1/lms/certificate-evaluations` | `CertificateEvaluationService/CreateCertificateEvaluation` | `lms.certificate_evaluation.create` |  | Create CertificateEvaluation. |
| 1072 | `GET /v1/lms/certificate-evaluations/{certificateEvaluationId}` | `CertificateEvaluationService/GetCertificateEvaluation` | `lms.certificate_evaluation.read` |  | Get CertificateEvaluation. |
| 1073 | `PATCH /v1/lms/certificate-evaluations/{certificateEvaluationId}` | `CertificateEvaluationService/UpdateCertificateEvaluation` | `lms.certificate_evaluation.update` |  | Update CertificateEvaluation. |
| 1074 | `DELETE /v1/lms/certificate-evaluations/{certificateEvaluationId}` | `CertificateEvaluationService/DeleteCertificateEvaluation` | `lms.certificate_evaluation.delete` |  | Delete CertificateEvaluation. |
| 1075 | `GET /v1/lms/badges` | `BadgeService/ListBadges` | `lms.badge.read` |  | List Badges. |
| 1076 | `POST /v1/lms/badges` | `BadgeService/CreateBadge` | `lms.badge.create` |  | Create Badge. |
| 1077 | `GET /v1/lms/badges/{badgeId}` | `BadgeService/GetBadge` | `lms.badge.read` |  | Get Badge. |
| 1078 | `PATCH /v1/lms/badges/{badgeId}` | `BadgeService/UpdateBadge` | `lms.badge.update` |  | Update Badge. |
| 1079 | `DELETE /v1/lms/badges/{badgeId}` | `BadgeService/DeleteBadge` | `lms.badge.delete` |  | Delete Badge. |
| 1080 | `GET /v1/lms/badge-awards` | `BadgeAwardService/ListBadgeAwards` | `lms.badge_award.read` |  | List BadgeAwards. |
| 1081 | `GET /v1/lms/badge-awards/{badgeAwardId}` | `BadgeAwardService/GetBadgeAward` | `lms.badge_award.read` |  | Get BadgeAward. |
| 1082 | `GET /v1/lms/settings` | `SettingsService/GetSettings` | `lms.settings.read` |  | Get Settings. |
| 1083 | `PATCH /v1/lms/settings` | `SettingsService/UpdateSettings` | `lms.settings.update` |  | Update Settings. |
| 1084 | `GET /v1/lms/settings/{settingsId}/sidebar-items` | `SettingsService/ListSettingsSidebarItems` | `lms.settings_sidebar_item.read` |  | List SettingsSidebarItems. |
| 1085 | `POST /v1/lms/settings/{settingsId}/sidebar-items` | `SettingsService/CreateSettingsSidebarItem` | `lms.settings_sidebar_item.create` |  | Create SettingsSidebarItem. |
| 1086 | `GET /v1/lms/settings/{settingsId}/sidebar-items/{settingsSidebarItemId}` | `SettingsService/GetSettingsSidebarItem` | `lms.settings_sidebar_item.read` |  | Get SettingsSidebarItem. |
| 1087 | `PATCH /v1/lms/settings/{settingsId}/sidebar-items/{settingsSidebarItemId}` | `SettingsService/UpdateSettingsSidebarItem` | `lms.settings_sidebar_item.update` |  | Update SettingsSidebarItem. |
| 1088 | `DELETE /v1/lms/settings/{settingsId}/sidebar-items/{settingsSidebarItemId}` | `SettingsService/DeleteSettingsSidebarItem` | `lms.settings_sidebar_item.delete` |  | Delete SettingsSidebarItem. |
| 1089 | `GET /v1/lms/settings/{settingsId}/primary-countries` | `SettingsService/ListSettingsPrimaryCountries` | `lms.settings_primary_country.read` |  | List SettingsPrimaryCountries. |
| 1090 | `POST /v1/lms/settings/{settingsId}/primary-countries` | `SettingsService/CreateSettingsPrimaryCountry` | `lms.settings_primary_country.create` |  | Create SettingsPrimaryCountry. |
| 1091 | `GET /v1/lms/settings/{settingsId}/primary-countries/{settingsPrimaryCountryId}` | `SettingsService/GetSettingsPrimaryCountry` | `lms.settings_primary_country.read` |  | Get SettingsPrimaryCountry. |
| 1092 | `PATCH /v1/lms/settings/{settingsId}/primary-countries/{settingsPrimaryCountryId}` | `SettingsService/UpdateSettingsPrimaryCountry` | `lms.settings_primary_country.update` |  | Update SettingsPrimaryCountry. |
| 1093 | `DELETE /v1/lms/settings/{settingsId}/primary-countries/{settingsPrimaryCountryId}` | `SettingsService/DeleteSettingsPrimaryCountry` | `lms.settings_primary_country.delete` |  | Delete SettingsPrimaryCountry. |
| 1094 | `GET /v1/lms/learning-objectives` | `LearningObjectiveService/ListLearningObjectives` | `lms.learning_objective.read` |  | List LearningObjectives. |
| 1095 | `POST /v1/lms/learning-objectives` | `LearningObjectiveService/CreateLearningObjective` | `lms.learning_objective.create` |  | Create LearningObjective. |
| 1096 | `GET /v1/lms/learning-objectives/{learningObjectiveId}` | `LearningObjectiveService/GetLearningObjective` | `lms.learning_objective.read` |  | Get LearningObjective. |
| 1097 | `PATCH /v1/lms/learning-objectives/{learningObjectiveId}` | `LearningObjectiveService/UpdateLearningObjective` | `lms.learning_objective.update` |  | Update LearningObjective. |
| 1098 | `DELETE /v1/lms/learning-objectives/{learningObjectiveId}` | `LearningObjectiveService/DeleteLearningObjective` | `lms.learning_objective.delete` |  | Delete LearningObjective. |
| 1099 | `GET /v1/lms/grading-categories` | `GradingCategoryService/ListGradingCategories` | `lms.grading_category.read` |  | List GradingCategories. |
| 1100 | `POST /v1/lms/grading-categories` | `GradingCategoryService/CreateGradingCategory` | `lms.grading_category.create` |  | Create GradingCategory. |
| 1101 | `GET /v1/lms/grading-categories/{gradingCategoryId}` | `GradingCategoryService/GetGradingCategory` | `lms.grading_category.read` |  | Get GradingCategory. |
| 1102 | `PATCH /v1/lms/grading-categories/{gradingCategoryId}` | `GradingCategoryService/UpdateGradingCategory` | `lms.grading_category.update` |  | Update GradingCategory. |
| 1103 | `DELETE /v1/lms/grading-categories/{gradingCategoryId}` | `GradingCategoryService/DeleteGradingCategory` | `lms.grading_category.delete` |  | Delete GradingCategory. |
| 1104 | `GET /v1/lms/content-items` | `ContentItemService/ListContentItems` | `lms.content_item.read` |  | List ContentItems. |
| 1105 | `POST /v1/lms/content-items` | `ContentItemService/CreateContentItem` | `lms.content_item.create` |  | Create ContentItem. |
| 1106 | `GET /v1/lms/content-items/{contentItemId}` | `ContentItemService/GetContentItem` | `lms.content_item.read` |  | Get ContentItem. |
| 1107 | `PATCH /v1/lms/content-items/{contentItemId}` | `ContentItemService/UpdateContentItem` | `lms.content_item.update` |  | Update ContentItem. |
| 1108 | `DELETE /v1/lms/content-items/{contentItemId}` | `ContentItemService/DeleteContentItem` | `lms.content_item.delete` |  | Delete ContentItem. |
| 1109 | `GET /v1/lms/content-items/{contentItemId}/ratings` | `ContentItemService/ListContentItemRatings` | `lms.content_item_rating.read` |  | List ContentItemRatings. |
| 1110 | `POST /v1/lms/content-items/{contentItemId}/ratings` | `ContentItemService/CreateContentItemRating` | `lms.content_item_rating.create` |  | Create ContentItemRating. |
| 1111 | `GET /v1/lms/content-items/{contentItemId}/ratings/{contentItemRatingId}` | `ContentItemService/GetContentItemRating` | `lms.content_item_rating.read` |  | Get ContentItemRating. |
| 1112 | `PATCH /v1/lms/content-items/{contentItemId}/ratings/{contentItemRatingId}` | `ContentItemService/UpdateContentItemRating` | `lms.content_item_rating.update` |  | Update ContentItemRating. |
| 1113 | `DELETE /v1/lms/content-items/{contentItemId}/ratings/{contentItemRatingId}` | `ContentItemService/DeleteContentItemRating` | `lms.content_item_rating.delete` |  | Delete ContentItemRating. |
| 1114 | `GET /v1/lms/content-items/{contentItemId}/overrides` | `ContentItemService/ListItemOverrides` | `lms.item_override.read` |  | List ItemOverrides. |
| 1115 | `POST /v1/lms/content-items/{contentItemId}/overrides` | `ContentItemService/CreateItemOverride` | `lms.item_override.create` |  | Create ItemOverride. |
| 1116 | `GET /v1/lms/content-items/{contentItemId}/overrides/{itemOverrideId}` | `ContentItemService/GetItemOverride` | `lms.item_override.read` |  | Get ItemOverride. |
| 1117 | `PATCH /v1/lms/content-items/{contentItemId}/overrides/{itemOverrideId}` | `ContentItemService/UpdateItemOverride` | `lms.item_override.update` |  | Update ItemOverride. |
| 1118 | `DELETE /v1/lms/content-items/{contentItemId}/overrides/{itemOverrideId}` | `ContentItemService/DeleteItemOverride` | `lms.item_override.delete` |  | Delete ItemOverride. |
| 1119 | `GET /v1/lms/gradebook-entries` | `GradebookEntryService/ListGradebookEntries` | `lms.gradebook_entry.read` |  | List GradebookEntries. |
| 1120 | `GET /v1/lms/gradebook-entries/{gradebookEntryId}` | `GradebookEntryService/GetGradebookEntry` | `lms.gradebook_entry.read` |  | Get GradebookEntry. |
| 1121 | `GET /v1/lms/grade-change-logs` | `GradeChangeLogService/ListGradeChangeLogs` | `lms.grade_change_log.read` |  | List GradeChangeLogs. |
| 1122 | `GET /v1/lms/grade-change-logs/{gradeChangeLogId}` | `GradeChangeLogService/GetGradeChangeLog` | `lms.grade_change_log.read` |  | Get GradeChangeLog. |
| 1123 | `GET /v1/lms/reassignment-logs` | `ReassignmentLogService/ListReassignmentLogs` | `lms.reassignment_log.read` |  | List ReassignmentLogs. |
| 1124 | `GET /v1/lms/reassignment-logs/{reassignmentLogId}` | `ReassignmentLogService/GetReassignmentLog` | `lms.reassignment_log.read` |  | Get ReassignmentLog. |
| 1125 | `GET /v1/lms/content-items/{contentItemId}/groups` | `ContentItemService/ListAssignmentGroups` | `lms.assignment_group.read` |  | List AssignmentGroups. |
| 1126 | `POST /v1/lms/content-items/{contentItemId}/groups` | `ContentItemService/CreateAssignmentGroup` | `lms.assignment_group.create` |  | Create AssignmentGroup. |
| 1127 | `GET /v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}` | `ContentItemService/GetAssignmentGroup` | `lms.assignment_group.read` |  | Get AssignmentGroup. |
| 1128 | `PATCH /v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}` | `ContentItemService/UpdateAssignmentGroup` | `lms.assignment_group.update` |  | Update AssignmentGroup. |
| 1129 | `DELETE /v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}` | `ContentItemService/DeleteAssignmentGroup` | `lms.assignment_group.delete` |  | Delete AssignmentGroup. |
| 1130 | `GET /v1/lms/assignment-group-members` | `AssignmentGroupMemberService/ListAssignmentGroupMembers` | `lms.assignment_group_member.read` |  | List AssignmentGroupMembers. |
| 1131 | `POST /v1/lms/assignment-group-members` | `AssignmentGroupMemberService/CreateAssignmentGroupMember` | `lms.assignment_group_member.create` |  | Create AssignmentGroupMember. |
| 1132 | `GET /v1/lms/assignment-group-members/{assignmentGroupMemberId}` | `AssignmentGroupMemberService/GetAssignmentGroupMember` | `lms.assignment_group_member.read` |  | Get AssignmentGroupMember. |
| 1133 | `PATCH /v1/lms/assignment-group-members/{assignmentGroupMemberId}` | `AssignmentGroupMemberService/UpdateAssignmentGroupMember` | `lms.assignment_group_member.update` |  | Update AssignmentGroupMember. |
| 1134 | `DELETE /v1/lms/assignment-group-members/{assignmentGroupMemberId}` | `AssignmentGroupMemberService/DeleteAssignmentGroupMember` | `lms.assignment_group_member.delete` |  | Delete AssignmentGroupMember. |
| 1135 | `GET /v1/lms/peer-reviews` | `PeerReviewService/ListPeerReviews` | `lms.peer_review.read` |  | List PeerReviews. |
| 1136 | `POST /v1/lms/peer-reviews` | `PeerReviewService/CreatePeerReview` | `lms.peer_review.create` |  | Create PeerReview. |
| 1137 | `GET /v1/lms/peer-reviews/{peerReviewId}` | `PeerReviewService/GetPeerReview` | `lms.peer_review.read` |  | Get PeerReview. |
| 1138 | `PATCH /v1/lms/peer-reviews/{peerReviewId}` | `PeerReviewService/UpdatePeerReview` | `lms.peer_review.update` |  | Update PeerReview. |
| 1139 | `DELETE /v1/lms/peer-reviews/{peerReviewId}` | `PeerReviewService/DeletePeerReview` | `lms.peer_review.delete` |  | Delete PeerReview. |
| 1140 | `GET /v1/lms/rubrics` | `RubricService/ListRubrics` | `lms.rubric.read` |  | List Rubrics. |
| 1141 | `POST /v1/lms/rubrics` | `RubricService/CreateRubric` | `lms.rubric.create` |  | Create Rubric. |
| 1142 | `GET /v1/lms/rubrics/{rubricId}` | `RubricService/GetRubric` | `lms.rubric.read` |  | Get Rubric. |
| 1143 | `PATCH /v1/lms/rubrics/{rubricId}` | `RubricService/UpdateRubric` | `lms.rubric.update` |  | Update Rubric. |
| 1144 | `DELETE /v1/lms/rubrics/{rubricId}` | `RubricService/DeleteRubric` | `lms.rubric.delete` |  | Delete Rubric. |
| 1145 | `GET /v1/lms/rubrics/{rubricId}/criteria` | `RubricService/ListRubricCriteria` | `lms.rubric_criterion.read` |  | List RubricCriteria. |
| 1146 | `POST /v1/lms/rubrics/{rubricId}/criteria` | `RubricService/CreateRubricCriterion` | `lms.rubric_criterion.create` |  | Create RubricCriterion. |
| 1147 | `GET /v1/lms/rubrics/{rubricId}/criteria/{rubricCriterionId}` | `RubricService/GetRubricCriterion` | `lms.rubric_criterion.read` |  | Get RubricCriterion. |
| 1148 | `PATCH /v1/lms/rubrics/{rubricId}/criteria/{rubricCriterionId}` | `RubricService/UpdateRubricCriterion` | `lms.rubric_criterion.update` |  | Update RubricCriterion. |
| 1149 | `DELETE /v1/lms/rubrics/{rubricId}/criteria/{rubricCriterionId}` | `RubricService/DeleteRubricCriterion` | `lms.rubric_criterion.delete` |  | Delete RubricCriterion. |
| 1150 | `GET /v1/lms/rubrics/{rubricId}/levels` | `RubricService/ListRubricLevels` | `lms.rubric_level.read` |  | List RubricLevels. |
| 1151 | `POST /v1/lms/rubrics/{rubricId}/levels` | `RubricService/CreateRubricLevel` | `lms.rubric_level.create` |  | Create RubricLevel. |
| 1152 | `GET /v1/lms/rubrics/{rubricId}/levels/{rubricLevelId}` | `RubricService/GetRubricLevel` | `lms.rubric_level.read` |  | Get RubricLevel. |
| 1153 | `PATCH /v1/lms/rubrics/{rubricId}/levels/{rubricLevelId}` | `RubricService/UpdateRubricLevel` | `lms.rubric_level.update` |  | Update RubricLevel. |
| 1154 | `DELETE /v1/lms/rubrics/{rubricId}/levels/{rubricLevelId}` | `RubricService/DeleteRubricLevel` | `lms.rubric_level.delete` |  | Delete RubricLevel. |
| 1155 | `GET /v1/lms/rubrics/{rubricId}/cells` | `RubricService/ListRubricCells` | `lms.rubric_cell.read` |  | List RubricCells. |
| 1156 | `POST /v1/lms/rubrics/{rubricId}/cells` | `RubricService/CreateRubricCell` | `lms.rubric_cell.create` |  | Create RubricCell. |
| 1157 | `GET /v1/lms/rubrics/{rubricId}/cells/{rubricCellId}` | `RubricService/GetRubricCell` | `lms.rubric_cell.read` |  | Get RubricCell. |
| 1158 | `PATCH /v1/lms/rubrics/{rubricId}/cells/{rubricCellId}` | `RubricService/UpdateRubricCell` | `lms.rubric_cell.update` |  | Update RubricCell. |
| 1159 | `DELETE /v1/lms/rubrics/{rubricId}/cells/{rubricCellId}` | `RubricService/DeleteRubricCell` | `lms.rubric_cell.delete` |  | Delete RubricCell. |
| 1160 | `GET /v1/lms/rubric-assessments` | `RubricAssessmentService/ListRubricAssessments` | `lms.rubric_assessment.read` |  | List RubricAssessments. |
| 1161 | `POST /v1/lms/rubric-assessments` | `RubricAssessmentService/CreateRubricAssessment` | `lms.rubric_assessment.create` |  | Create RubricAssessment. |
| 1162 | `GET /v1/lms/rubric-assessments/{rubricAssessmentId}` | `RubricAssessmentService/GetRubricAssessment` | `lms.rubric_assessment.read` |  | Get RubricAssessment. |
| 1163 | `PATCH /v1/lms/rubric-assessments/{rubricAssessmentId}` | `RubricAssessmentService/UpdateRubricAssessment` | `lms.rubric_assessment.update` |  | Update RubricAssessment. |
| 1164 | `DELETE /v1/lms/rubric-assessments/{rubricAssessmentId}` | `RubricAssessmentService/DeleteRubricAssessment` | `lms.rubric_assessment.delete` |  | Delete RubricAssessment. |
| 1165 | `GET /v1/lms/discussion-prompts` | `DiscussionPromptService/ListDiscussionPrompts` | `lms.discussion_prompt.read` |  | List DiscussionPrompts. |
| 1166 | `POST /v1/lms/discussion-prompts` | `DiscussionPromptService/CreateDiscussionPrompt` | `lms.discussion_prompt.create` |  | Create DiscussionPrompt. |
| 1167 | `GET /v1/lms/discussion-prompts/{discussionPromptId}` | `DiscussionPromptService/GetDiscussionPrompt` | `lms.discussion_prompt.read` |  | Get DiscussionPrompt. |
| 1168 | `PATCH /v1/lms/discussion-prompts/{discussionPromptId}` | `DiscussionPromptService/UpdateDiscussionPrompt` | `lms.discussion_prompt.update` |  | Update DiscussionPrompt. |
| 1169 | `DELETE /v1/lms/discussion-prompts/{discussionPromptId}` | `DiscussionPromptService/DeleteDiscussionPrompt` | `lms.discussion_prompt.delete` |  | Delete DiscussionPrompt. |
| 1170 | `GET /v1/lms/participation-activities` | `ParticipationActivityService/ListParticipationActivities` | `lms.participation_activity.read` |  | List ParticipationActivities. |
| 1171 | `POST /v1/lms/participation-activities` | `ParticipationActivityService/CreateParticipationActivity` | `lms.participation_activity.create` |  | Create ParticipationActivity. |
| 1172 | `GET /v1/lms/participation-activities/{participationActivityId}` | `ParticipationActivityService/GetParticipationActivity` | `lms.participation_activity.read` |  | Get ParticipationActivity. |
| 1173 | `PATCH /v1/lms/participation-activities/{participationActivityId}` | `ParticipationActivityService/UpdateParticipationActivity` | `lms.participation_activity.update` |  | Update ParticipationActivity. |
| 1174 | `DELETE /v1/lms/participation-activities/{participationActivityId}` | `ParticipationActivityService/DeleteParticipationActivity` | `lms.participation_activity.delete` |  | Delete ParticipationActivity. |
| 1175 | `GET /v1/lms/resource-categories` | `ResourceCategoryService/ListResourceCategories` | `lms.resource_category.read` |  | List ResourceCategories. |
| 1176 | `POST /v1/lms/resource-categories` | `ResourceCategoryService/CreateResourceCategory` | `lms.resource_category.create` |  | Create ResourceCategory. |
| 1177 | `GET /v1/lms/resource-categories/{resourceCategoryId}` | `ResourceCategoryService/GetResourceCategory` | `lms.resource_category.read` |  | Get ResourceCategory. |
| 1178 | `PATCH /v1/lms/resource-categories/{resourceCategoryId}` | `ResourceCategoryService/UpdateResourceCategory` | `lms.resource_category.update` |  | Update ResourceCategory. |
| 1179 | `DELETE /v1/lms/resource-categories/{resourceCategoryId}` | `ResourceCategoryService/DeleteResourceCategory` | `lms.resource_category.delete` |  | Delete ResourceCategory. |
| 1180 | `GET /v1/lms/web-resources` | `WebResourceService/ListWebResources` | `lms.web_resource.read` |  | List WebResources. |
| 1181 | `POST /v1/lms/web-resources` | `WebResourceService/CreateWebResource` | `lms.web_resource.create` |  | Create WebResource. |
| 1182 | `GET /v1/lms/web-resources/{webResourceId}` | `WebResourceService/GetWebResource` | `lms.web_resource.read` |  | Get WebResource. |
| 1183 | `PATCH /v1/lms/web-resources/{webResourceId}` | `WebResourceService/UpdateWebResource` | `lms.web_resource.update` |  | Update WebResource. |
| 1184 | `DELETE /v1/lms/web-resources/{webResourceId}` | `WebResourceService/DeleteWebResource` | `lms.web_resource.delete` |  | Delete WebResource. |
| 1185 | `GET /v1/lms/ebooks` | `EbookService/ListEbooks` | `lms.ebook.read` |  | List Ebooks. |
| 1186 | `POST /v1/lms/ebooks` | `EbookService/CreateEbook` | `lms.ebook.create` |  | Create Ebook. |
| 1187 | `GET /v1/lms/ebooks/{ebookId}` | `EbookService/GetEbook` | `lms.ebook.read` |  | Get Ebook. |
| 1188 | `PATCH /v1/lms/ebooks/{ebookId}` | `EbookService/UpdateEbook` | `lms.ebook.update` |  | Update Ebook. |
| 1189 | `DELETE /v1/lms/ebooks/{ebookId}` | `EbookService/DeleteEbook` | `lms.ebook.delete` |  | Delete Ebook. |
| 1190 | `GET /v1/lms/ebooks/{ebookId}/related-materials` | `EbookService/ListRelatedMaterials` | `lms.related_material.read` |  | List RelatedMaterials. |
| 1191 | `POST /v1/lms/ebooks/{ebookId}/related-materials` | `EbookService/CreateRelatedMaterial` | `lms.related_material.create` |  | Create RelatedMaterial. |
| 1192 | `GET /v1/lms/ebooks/{ebookId}/related-materials/{relatedMaterialId}` | `EbookService/GetRelatedMaterial` | `lms.related_material.read` |  | Get RelatedMaterial. |
| 1193 | `PATCH /v1/lms/ebooks/{ebookId}/related-materials/{relatedMaterialId}` | `EbookService/UpdateRelatedMaterial` | `lms.related_material.update` |  | Update RelatedMaterial. |
| 1194 | `DELETE /v1/lms/ebooks/{ebookId}/related-materials/{relatedMaterialId}` | `EbookService/DeleteRelatedMaterial` | `lms.related_material.delete` |  | Delete RelatedMaterial. |
| 1195 | `GET /v1/lms/ebooks/{ebookId}/related-material-ranks` | `EbookService/ListRelatedMaterialRanks` | `lms.related_material_rank.read` |  | List RelatedMaterialRanks. |
| 1196 | `GET /v1/lms/ebooks/{ebookId}/related-material-ranks/{relatedMaterialRankId}` | `EbookService/GetRelatedMaterialRank` | `lms.related_material_rank.read` |  | Get RelatedMaterialRank. |
| 1197 | `GET /v1/lms/annotations` | `AnnotationService/ListAnnotations` | `lms.annotation.read` |  | List Annotations. |
| 1198 | `POST /v1/lms/annotations` | `AnnotationService/CreateAnnotation` | `lms.annotation.create` |  | Create Annotation. |
| 1199 | `GET /v1/lms/annotations/{annotationId}` | `AnnotationService/GetAnnotation` | `lms.annotation.read` |  | Get Annotation. |
| 1200 | `PATCH /v1/lms/annotations/{annotationId}` | `AnnotationService/UpdateAnnotation` | `lms.annotation.update` |  | Update Annotation. |
| 1201 | `DELETE /v1/lms/annotations/{annotationId}` | `AnnotationService/DeleteAnnotation` | `lms.annotation.delete` |  | Delete Annotation. |
| 1202 | `GET /v1/lms/annotations/{annotationId}/shares` | `AnnotationService/ListAnnotationShares` | `lms.annotation_share.read` |  | List AnnotationShares. |
| 1203 | `POST /v1/lms/annotations/{annotationId}/shares` | `AnnotationService/CreateAnnotationShare` | `lms.annotation_share.create` |  | Create AnnotationShare. |
| 1204 | `GET /v1/lms/annotations/{annotationId}/shares/{annotationShareId}` | `AnnotationService/GetAnnotationShare` | `lms.annotation_share.read` |  | Get AnnotationShare. |
| 1205 | `PATCH /v1/lms/annotations/{annotationId}/shares/{annotationShareId}` | `AnnotationService/UpdateAnnotationShare` | `lms.annotation_share.update` |  | Update AnnotationShare. |
| 1206 | `DELETE /v1/lms/annotations/{annotationId}/shares/{annotationShareId}` | `AnnotationService/DeleteAnnotationShare` | `lms.annotation_share.delete` |  | Delete AnnotationShare. |
| 1207 | `GET /v1/lms/external-tools` | `ExternalToolService/ListExternalTools` | `lms.external_tool.read` |  | List ExternalTools. |
| 1208 | `POST /v1/lms/external-tools` | `ExternalToolService/CreateExternalTool` | `lms.external_tool.create` |  | Create ExternalTool. |
| 1209 | `GET /v1/lms/external-tools/{externalToolId}` | `ExternalToolService/GetExternalTool` | `lms.external_tool.read` |  | Get ExternalTool. |
| 1210 | `PATCH /v1/lms/external-tools/{externalToolId}` | `ExternalToolService/UpdateExternalTool` | `lms.external_tool.update` |  | Update ExternalTool. |
| 1211 | `DELETE /v1/lms/external-tools/{externalToolId}` | `ExternalToolService/DeleteExternalTool` | `lms.external_tool.delete` |  | Delete ExternalTool. |
| 1212 | `GET /v1/lms/external-tools/{externalToolId}/settings` | `ExternalToolService/ListExternalToolSettings` | `lms.external_tool_setting.read` |  | List ExternalToolSettings. |
| 1213 | `POST /v1/lms/external-tools/{externalToolId}/settings` | `ExternalToolService/CreateExternalToolSetting` | `lms.external_tool_setting.create` |  | Create ExternalToolSetting. |
| 1214 | `GET /v1/lms/external-tools/{externalToolId}/settings/{externalToolSettingId}` | `ExternalToolService/GetExternalToolSetting` | `lms.external_tool_setting.read` |  | Get ExternalToolSetting. |
| 1215 | `PATCH /v1/lms/external-tools/{externalToolId}/settings/{externalToolSettingId}` | `ExternalToolService/UpdateExternalToolSetting` | `lms.external_tool_setting.update` |  | Update ExternalToolSetting. |
| 1216 | `DELETE /v1/lms/external-tools/{externalToolId}/settings/{externalToolSettingId}` | `ExternalToolService/DeleteExternalToolSetting` | `lms.external_tool_setting.delete` |  | Delete ExternalToolSetting. |
| 1217 | `GET /v1/lms/external-tools/{externalToolId}/institutions` | `ExternalToolService/ListExternalToolInstitutions` | `lms.external_tool_institution.read` |  | List ExternalToolInstitutions. |
| 1218 | `POST /v1/lms/external-tools/{externalToolId}/institutions` | `ExternalToolService/CreateExternalToolInstitution` | `lms.external_tool_institution.create` |  | Create ExternalToolInstitution. |
| 1219 | `GET /v1/lms/external-tools/{externalToolId}/institutions/{externalToolInstitutionId}` | `ExternalToolService/GetExternalToolInstitution` | `lms.external_tool_institution.read` |  | Get ExternalToolInstitution. |
| 1220 | `PATCH /v1/lms/external-tools/{externalToolId}/institutions/{externalToolInstitutionId}` | `ExternalToolService/UpdateExternalToolInstitution` | `lms.external_tool_institution.update` |  | Update ExternalToolInstitution. |
| 1221 | `DELETE /v1/lms/external-tools/{externalToolId}/institutions/{externalToolInstitutionId}` | `ExternalToolService/DeleteExternalToolInstitution` | `lms.external_tool_institution.delete` |  | Delete ExternalToolInstitution. |
| 1222 | `GET /v1/lms/external-tools/{externalToolId}/roles` | `ExternalToolService/ListExternalToolRoles` | `lms.external_tool_role.read` |  | List ExternalToolRoles. |
| 1223 | `POST /v1/lms/external-tools/{externalToolId}/roles` | `ExternalToolService/CreateExternalToolRole` | `lms.external_tool_role.create` |  | Create ExternalToolRole. |
| 1224 | `GET /v1/lms/external-tools/{externalToolId}/roles/{externalToolRoleId}` | `ExternalToolService/GetExternalToolRole` | `lms.external_tool_role.read` |  | Get ExternalToolRole. |
| 1225 | `PATCH /v1/lms/external-tools/{externalToolId}/roles/{externalToolRoleId}` | `ExternalToolService/UpdateExternalToolRole` | `lms.external_tool_role.update` |  | Update ExternalToolRole. |
| 1226 | `DELETE /v1/lms/external-tools/{externalToolId}/roles/{externalToolRoleId}` | `ExternalToolService/DeleteExternalToolRole` | `lms.external_tool_role.delete` |  | Delete ExternalToolRole. |
| 1227 | `GET /v1/lms/external-tools/{externalToolId}/options` | `ExternalToolService/ListExternalToolOptions` | `lms.external_tool_option.read` |  | List ExternalToolOptions. |
| 1228 | `POST /v1/lms/external-tools/{externalToolId}/options` | `ExternalToolService/CreateExternalToolOption` | `lms.external_tool_option.create` |  | Create ExternalToolOption. |
| 1229 | `GET /v1/lms/external-tools/{externalToolId}/options/{externalToolOptionId}` | `ExternalToolService/GetExternalToolOption` | `lms.external_tool_option.read` |  | Get ExternalToolOption. |
| 1230 | `PATCH /v1/lms/external-tools/{externalToolId}/options/{externalToolOptionId}` | `ExternalToolService/UpdateExternalToolOption` | `lms.external_tool_option.update` |  | Update ExternalToolOption. |
| 1231 | `DELETE /v1/lms/external-tools/{externalToolId}/options/{externalToolOptionId}` | `ExternalToolService/DeleteExternalToolOption` | `lms.external_tool_option.delete` |  | Delete ExternalToolOption. |
| 1232 | `GET /v1/lms/external-tool-resources` | `ExternalToolResourceService/ListExternalToolResources` | `lms.external_tool_resource.read` |  | List ExternalToolResources. |
| 1233 | `POST /v1/lms/external-tool-resources` | `ExternalToolResourceService/CreateExternalToolResource` | `lms.external_tool_resource.create` |  | Create ExternalToolResource. |
| 1234 | `GET /v1/lms/external-tool-resources/{externalToolResourceId}` | `ExternalToolResourceService/GetExternalToolResource` | `lms.external_tool_resource.read` |  | Get ExternalToolResource. |
| 1235 | `PATCH /v1/lms/external-tool-resources/{externalToolResourceId}` | `ExternalToolResourceService/UpdateExternalToolResource` | `lms.external_tool_resource.update` |  | Update ExternalToolResource. |
| 1236 | `DELETE /v1/lms/external-tool-resources/{externalToolResourceId}` | `ExternalToolResourceService/DeleteExternalToolResource` | `lms.external_tool_resource.delete` |  | Delete ExternalToolResource. |
| 1237 | `GET /v1/lms/external-tool-resources/{externalToolResourceId}/links` | `ExternalToolResourceService/ListExternalToolLinks` | `lms.external_tool_link.read` |  | List ExternalToolLinks. |
| 1238 | `POST /v1/lms/external-tool-resources/{externalToolResourceId}/links` | `ExternalToolResourceService/CreateExternalToolLink` | `lms.external_tool_link.create` |  | Create ExternalToolLink. |
| 1239 | `GET /v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId}` | `ExternalToolResourceService/GetExternalToolLink` | `lms.external_tool_link.read` |  | Get ExternalToolLink. |
| 1240 | `PATCH /v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId}` | `ExternalToolResourceService/UpdateExternalToolLink` | `lms.external_tool_link.update` |  | Update ExternalToolLink. |
| 1241 | `DELETE /v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId}` | `ExternalToolResourceService/DeleteExternalToolLink` | `lms.external_tool_link.delete` |  | Delete ExternalToolLink. |
| 1242 | `GET /v1/lms/cohorts/{cohortId}/forums` | `CohortService/ListForums` | `lms.forum.read` |  | List Forums. |
| 1243 | `POST /v1/lms/cohorts/{cohortId}/forums` | `CohortService/CreateForum` | `lms.forum.create` |  | Create Forum. |
| 1244 | `GET /v1/lms/cohorts/{cohortId}/forums/{forumId}` | `CohortService/GetForum` | `lms.forum.read` |  | Get Forum. |
| 1245 | `PATCH /v1/lms/cohorts/{cohortId}/forums/{forumId}` | `CohortService/UpdateForum` | `lms.forum.update` |  | Update Forum. |
| 1246 | `DELETE /v1/lms/cohorts/{cohortId}/forums/{forumId}` | `CohortService/DeleteForum` | `lms.forum.delete` |  | Delete Forum. |
| 1247 | `GET /v1/lms/forum-sections` | `ForumSectionService/ListForumSections` | `lms.forum_section.read` |  | List ForumSections. |
| 1248 | `POST /v1/lms/forum-sections` | `ForumSectionService/CreateForumSection` | `lms.forum_section.create` |  | Create ForumSection. |
| 1249 | `GET /v1/lms/forum-sections/{forumSectionId}` | `ForumSectionService/GetForumSection` | `lms.forum_section.read` |  | Get ForumSection. |
| 1250 | `PATCH /v1/lms/forum-sections/{forumSectionId}` | `ForumSectionService/UpdateForumSection` | `lms.forum_section.update` |  | Update ForumSection. |
| 1251 | `DELETE /v1/lms/forum-sections/{forumSectionId}` | `ForumSectionService/DeleteForumSection` | `lms.forum_section.delete` |  | Delete ForumSection. |
| 1252 | `GET /v1/lms/forum-topics` | `ForumTopicService/ListForumTopics` | `lms.forum_topic.read` |  | List ForumTopics. |
| 1253 | `POST /v1/lms/forum-topics` | `ForumTopicService/CreateForumTopic` | `lms.forum_topic.create` |  | Create ForumTopic. |
| 1254 | `GET /v1/lms/forum-topics/{forumTopicId}` | `ForumTopicService/GetForumTopic` | `lms.forum_topic.read` |  | Get ForumTopic. |
| 1255 | `PATCH /v1/lms/forum-topics/{forumTopicId}` | `ForumTopicService/UpdateForumTopic` | `lms.forum_topic.update` |  | Update ForumTopic. |
| 1256 | `DELETE /v1/lms/forum-topics/{forumTopicId}` | `ForumTopicService/DeleteForumTopic` | `lms.forum_topic.delete` |  | Delete ForumTopic. |
| 1257 | `GET /v1/lms/forum-topics/{forumTopicId}/posts` | `ForumTopicService/ListForumPosts` | `lms.forum_post.read` |  | List ForumPosts. |
| 1258 | `POST /v1/lms/forum-topics/{forumTopicId}/posts` | `ForumTopicService/CreateForumPost` | `lms.forum_post.create` |  | Create ForumPost. |
| 1259 | `GET /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}` | `ForumTopicService/GetForumPost` | `lms.forum_post.read` |  | Get ForumPost. |
| 1260 | `PATCH /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}` | `ForumTopicService/UpdateForumPost` | `lms.forum_post.update` |  | Update ForumPost. |
| 1261 | `DELETE /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}` | `ForumTopicService/DeleteForumPost` | `lms.forum_post.delete` |  | Delete ForumPost. |
| 1262 | `GET /v1/lms/forum-post-votes` | `ForumPostVoteService/ListForumPostVotes` | `lms.forum_post_vote.read` |  | List ForumPostVotes. |
| 1263 | `POST /v1/lms/forum-post-votes` | `ForumPostVoteService/CreateForumPostVote` | `lms.forum_post_vote.create` |  | Create ForumPostVote. |
| 1264 | `PATCH /v1/lms/forum-post-votes/{forumPostVoteId}` | `ForumPostVoteService/UpdateForumPostVote` | `lms.forum_post_vote.update` |  | Update ForumPostVote. |
| 1265 | `DELETE /v1/lms/forum-post-votes/{forumPostVoteId}` | `ForumPostVoteService/DeleteForumPostVote` | `lms.forum_post_vote.delete` |  | Delete ForumPostVote. |
| 1266 | `GET /v1/lms/forum-report-categories` | `ForumReportCategoryService/ListForumReportCategories` | `lms.forum_report_category.read` |  | List ForumReportCategories. |
| 1267 | `POST /v1/lms/forum-report-categories` | `ForumReportCategoryService/CreateForumReportCategory` | `lms.forum_report_category.create` |  | Create ForumReportCategory. |
| 1268 | `GET /v1/lms/forum-report-categories/{forumReportCategoryId}` | `ForumReportCategoryService/GetForumReportCategory` | `lms.forum_report_category.read` |  | Get ForumReportCategory. |
| 1269 | `PATCH /v1/lms/forum-report-categories/{forumReportCategoryId}` | `ForumReportCategoryService/UpdateForumReportCategory` | `lms.forum_report_category.update` |  | Update ForumReportCategory. |
| 1270 | `DELETE /v1/lms/forum-report-categories/{forumReportCategoryId}` | `ForumReportCategoryService/DeleteForumReportCategory` | `lms.forum_report_category.delete` |  | Delete ForumReportCategory. |
| 1271 | `GET /v1/lms/forum-post-reports` | `ForumPostReportService/ListForumPostReports` | `lms.forum_post_report.read` |  | List ForumPostReports. |
| 1272 | `POST /v1/lms/forum-post-reports` | `ForumPostReportService/CreateForumPostReport` | `lms.forum_post_report.create` |  | Create ForumPostReport. |
| 1273 | `GET /v1/lms/forum-post-reports/{forumPostReportId}` | `ForumPostReportService/GetForumPostReport` | `lms.forum_post_report.read` |  | Get ForumPostReport. |
| 1274 | `PATCH /v1/lms/forum-post-reports/{forumPostReportId}` | `ForumPostReportService/UpdateForumPostReport` | `lms.forum_post_report.update` |  | Update ForumPostReport. |
| 1275 | `DELETE /v1/lms/forum-post-reports/{forumPostReportId}` | `ForumPostReportService/DeleteForumPostReport` | `lms.forum_post_report.delete` |  | Delete ForumPostReport. |
| 1276 | `GET /v1/lms/forum-topics/{forumTopicId}/read-states` | `ForumTopicService/ListForumReadStates` | `lms.forum_read_state.read` |  | List ForumReadStates. |
| 1277 | `GET /v1/lms/cohorts/{cohortId}/groups` | `CohortService/ListCohortGroups` | `lms.cohort_group.read` |  | List CohortGroups. |
| 1278 | `POST /v1/lms/cohorts/{cohortId}/groups` | `CohortService/CreateCohortGroup` | `lms.cohort_group.create` |  | Create CohortGroup. |
| 1279 | `GET /v1/lms/cohorts/{cohortId}/groups/{cohortGroupId}` | `CohortService/GetCohortGroup` | `lms.cohort_group.read` |  | Get CohortGroup. |
| 1280 | `PATCH /v1/lms/cohorts/{cohortId}/groups/{cohortGroupId}` | `CohortService/UpdateCohortGroup` | `lms.cohort_group.update` |  | Update CohortGroup. |
| 1281 | `DELETE /v1/lms/cohorts/{cohortId}/groups/{cohortGroupId}` | `CohortService/DeleteCohortGroup` | `lms.cohort_group.delete` |  | Delete CohortGroup. |
| 1282 | `GET /v1/lms/cohorts/{cohortId}/schedule-slots` | `CohortService/ListCohortScheduleSlots` | `lms.cohort_schedule_slot.read` |  | List CohortScheduleSlots. |
| 1283 | `POST /v1/lms/cohorts/{cohortId}/schedule-slots` | `CohortService/CreateCohortScheduleSlot` | `lms.cohort_schedule_slot.create` |  | Create CohortScheduleSlot. |
| 1284 | `GET /v1/lms/cohorts/{cohortId}/schedule-slots/{cohortScheduleSlotId}` | `CohortService/GetCohortScheduleSlot` | `lms.cohort_schedule_slot.read` |  | Get CohortScheduleSlot. |
| 1285 | `PATCH /v1/lms/cohorts/{cohortId}/schedule-slots/{cohortScheduleSlotId}` | `CohortService/UpdateCohortScheduleSlot` | `lms.cohort_schedule_slot.update` |  | Update CohortScheduleSlot. |
| 1286 | `DELETE /v1/lms/cohorts/{cohortId}/schedule-slots/{cohortScheduleSlotId}` | `CohortService/DeleteCohortScheduleSlot` | `lms.cohort_schedule_slot.delete` |  | Delete CohortScheduleSlot. |
| 1287 | `GET /v1/lms/cohorts/{cohortId}/sessions` | `CohortService/ListCohortSessions` | `lms.cohort_session.read` |  | List CohortSessions. |
| 1288 | `POST /v1/lms/cohorts/{cohortId}/sessions` | `CohortService/CreateCohortSession` | `lms.cohort_session.create` |  | Create CohortSession. |
| 1289 | `GET /v1/lms/cohorts/{cohortId}/sessions/{cohortSessionId}` | `CohortService/GetCohortSession` | `lms.cohort_session.read` |  | Get CohortSession. |
| 1290 | `PATCH /v1/lms/cohorts/{cohortId}/sessions/{cohortSessionId}` | `CohortService/UpdateCohortSession` | `lms.cohort_session.update` |  | Update CohortSession. |
| 1291 | `DELETE /v1/lms/cohorts/{cohortId}/sessions/{cohortSessionId}` | `CohortService/DeleteCohortSession` | `lms.cohort_session.delete` |  | Delete CohortSession. |
| 1292 | `GET /v1/lms/cohorts/{cohortId}/holidays` | `CohortService/ListCohortHolidays` | `lms.cohort_holiday.read` |  | List CohortHolidays. |
| 1293 | `POST /v1/lms/cohorts/{cohortId}/holidays` | `CohortService/CreateCohortHoliday` | `lms.cohort_holiday.create` |  | Create CohortHoliday. |
| 1294 | `GET /v1/lms/cohorts/{cohortId}/holidays/{cohortHolidayId}` | `CohortService/GetCohortHoliday` | `lms.cohort_holiday.read` |  | Get CohortHoliday. |
| 1295 | `PATCH /v1/lms/cohorts/{cohortId}/holidays/{cohortHolidayId}` | `CohortService/UpdateCohortHoliday` | `lms.cohort_holiday.update` |  | Update CohortHoliday. |
| 1296 | `DELETE /v1/lms/cohorts/{cohortId}/holidays/{cohortHolidayId}` | `CohortService/DeleteCohortHoliday` | `lms.cohort_holiday.delete` |  | Delete CohortHoliday. |
| 1297 | `GET /v1/lms/courses/{courseId}/author-profiles` | `LmsCourseService/ListCourseAuthorProfiles` | `lms.course_author_profile.read` | Public | List CourseAuthorProfiles. |
| 1298 | `POST /v1/lms/courses/{courseId}/author-profiles` | `LmsCourseService/CreateCourseAuthorProfile` | `lms.course_author_profile.create` |  | Create CourseAuthorProfile. |
| 1299 | `GET /v1/lms/courses/{courseId}/author-profiles/{courseAuthorProfileId}` | `LmsCourseService/GetCourseAuthorProfile` | `lms.course_author_profile.read` |  | Get CourseAuthorProfile. |
| 1300 | `PATCH /v1/lms/courses/{courseId}/author-profiles/{courseAuthorProfileId}` | `LmsCourseService/UpdateCourseAuthorProfile` | `lms.course_author_profile.update` |  | Update CourseAuthorProfile. |
| 1301 | `DELETE /v1/lms/courses/{courseId}/author-profiles/{courseAuthorProfileId}` | `LmsCourseService/DeleteCourseAuthorProfile` | `lms.course_author_profile.delete` |  | Delete CourseAuthorProfile. |
| 1302 | `GET /v1/lms/courses/{courseId}/previews` | `LmsCourseService/ListCoursePreviews` | `lms.course_preview.read` | Public | List CoursePreviews. |
| 1303 | `POST /v1/lms/courses/{courseId}/previews` | `LmsCourseService/CreateCoursePreview` | `lms.course_preview.create` |  | Create CoursePreview. |
| 1304 | `GET /v1/lms/courses/{courseId}/previews/{coursePreviewId}` | `LmsCourseService/GetCoursePreview` | `lms.course_preview.read` |  | Get CoursePreview. |
| 1305 | `PATCH /v1/lms/courses/{courseId}/previews/{coursePreviewId}` | `LmsCourseService/UpdateCoursePreview` | `lms.course_preview.update` |  | Update CoursePreview. |
| 1306 | `DELETE /v1/lms/courses/{courseId}/previews/{coursePreviewId}` | `LmsCourseService/DeleteCoursePreview` | `lms.course_preview.delete` |  | Delete CoursePreview. |
| 1307 | `GET /v1/lms/courses/{courseId}/testimonials` | `LmsCourseService/ListCourseTestimonials` | `lms.course_testimonial.read` | Public | List CourseTestimonials. |
| 1308 | `POST /v1/lms/courses/{courseId}/testimonials` | `LmsCourseService/CreateCourseTestimonial` | `lms.course_testimonial.create` |  | Create CourseTestimonial. |
| 1309 | `GET /v1/lms/courses/{courseId}/testimonials/{courseTestimonialId}` | `LmsCourseService/GetCourseTestimonial` | `lms.course_testimonial.read` |  | Get CourseTestimonial. |
| 1310 | `PATCH /v1/lms/courses/{courseId}/testimonials/{courseTestimonialId}` | `LmsCourseService/UpdateCourseTestimonial` | `lms.course_testimonial.update` |  | Update CourseTestimonial. |
| 1311 | `DELETE /v1/lms/courses/{courseId}/testimonials/{courseTestimonialId}` | `LmsCourseService/DeleteCourseTestimonial` | `lms.course_testimonial.delete` |  | Delete CourseTestimonial. |
| 1312 | `GET /v1/lms/courses/{courseId}/banners` | `LmsCourseService/ListCatalogBanners` | `lms.catalog_banner.read` | Public | List CatalogBanners. |
| 1313 | `POST /v1/lms/courses/{courseId}/banners` | `LmsCourseService/CreateCatalogBanner` | `lms.catalog_banner.create` |  | Create CatalogBanner. |
| 1314 | `GET /v1/lms/courses/{courseId}/banners/{catalogBannerId}` | `LmsCourseService/GetCatalogBanner` | `lms.catalog_banner.read` |  | Get CatalogBanner. |
| 1315 | `PATCH /v1/lms/courses/{courseId}/banners/{catalogBannerId}` | `LmsCourseService/UpdateCatalogBanner` | `lms.catalog_banner.update` |  | Update CatalogBanner. |
| 1316 | `DELETE /v1/lms/courses/{courseId}/banners/{catalogBannerId}` | `LmsCourseService/DeleteCatalogBanner` | `lms.catalog_banner.delete` |  | Delete CatalogBanner. |
| 1317 | `GET /v1/lms/courses/{courseId}/enrollment-requests` | `LmsCourseService/ListEnrollmentRequests` | `lms.enrollment_request.read` |  | List EnrollmentRequests. |
| 1318 | `POST /v1/lms/courses/{courseId}/enrollment-requests` | `LmsCourseService/CreateEnrollmentRequest` | `lms.enrollment_request.create` |  | Create EnrollmentRequest. |
| 1319 | `GET /v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}` | `LmsCourseService/GetEnrollmentRequest` | `lms.enrollment_request.read` |  | Get EnrollmentRequest. |
| 1320 | `PATCH /v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}` | `LmsCourseService/UpdateEnrollmentRequest` | `lms.enrollment_request.update` |  | Update EnrollmentRequest. |
| 1321 | `DELETE /v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}` | `LmsCourseService/DeleteEnrollmentRequest` | `lms.enrollment_request.delete` |  | Delete EnrollmentRequest. |
| 1322 | `GET /v1/lms/release-rules` | `ReleaseRuleService/ListReleaseRules` | `lms.release_rule.read` |  | List ReleaseRules. |
| 1323 | `POST /v1/lms/release-rules` | `ReleaseRuleService/CreateReleaseRule` | `lms.release_rule.create` |  | Create ReleaseRule. |
| 1324 | `GET /v1/lms/release-rules/{releaseRuleId}` | `ReleaseRuleService/GetReleaseRule` | `lms.release_rule.read` |  | Get ReleaseRule. |
| 1325 | `PATCH /v1/lms/release-rules/{releaseRuleId}` | `ReleaseRuleService/UpdateReleaseRule` | `lms.release_rule.update` |  | Update ReleaseRule. |
| 1326 | `DELETE /v1/lms/release-rules/{releaseRuleId}` | `ReleaseRuleService/DeleteReleaseRule` | `lms.release_rule.delete` |  | Delete ReleaseRule. |
| 1327 | `GET /v1/lms/release-rules/{releaseRuleId}/release-rule-conditions` | `ReleaseRuleService/ListReleaseRuleConditions` | `lms.release_rule_condition.read` |  | List ReleaseRuleConditions. |
| 1328 | `POST /v1/lms/release-rules/{releaseRuleId}/release-rule-conditions` | `ReleaseRuleService/CreateReleaseRuleCondition` | `lms.release_rule_condition.create` |  | Create ReleaseRuleCondition. |
| 1329 | `GET /v1/lms/release-rules/{releaseRuleId}/release-rule-conditions/{releaseRuleConditionId}` | `ReleaseRuleService/GetReleaseRuleCondition` | `lms.release_rule_condition.read` |  | Get ReleaseRuleCondition. |
| 1330 | `PATCH /v1/lms/release-rules/{releaseRuleId}/release-rule-conditions/{releaseRuleConditionId}` | `ReleaseRuleService/UpdateReleaseRuleCondition` | `lms.release_rule_condition.update` |  | Update ReleaseRuleCondition. |
| 1331 | `DELETE /v1/lms/release-rules/{releaseRuleId}/release-rule-conditions/{releaseRuleConditionId}` | `ReleaseRuleService/DeleteReleaseRuleCondition` | `lms.release_rule_condition.delete` |  | Delete ReleaseRuleCondition. |
| 1332 | `GET /v1/lms/release-rules/{releaseRuleId}/release-rule-targets` | `ReleaseRuleService/ListReleaseRuleTargets` | `lms.release_rule_target.read` |  | List ReleaseRuleTargets. |
| 1333 | `POST /v1/lms/release-rules/{releaseRuleId}/release-rule-targets` | `ReleaseRuleService/CreateReleaseRuleTarget` | `lms.release_rule_target.create` |  | Create ReleaseRuleTarget. |
| 1334 | `GET /v1/lms/release-rules/{releaseRuleId}/release-rule-targets/{releaseRuleTargetId}` | `ReleaseRuleService/GetReleaseRuleTarget` | `lms.release_rule_target.read` |  | Get ReleaseRuleTarget. |
| 1335 | `PATCH /v1/lms/release-rules/{releaseRuleId}/release-rule-targets/{releaseRuleTargetId}` | `ReleaseRuleService/UpdateReleaseRuleTarget` | `lms.release_rule_target.update` |  | Update ReleaseRuleTarget. |
| 1336 | `DELETE /v1/lms/release-rules/{releaseRuleId}/release-rule-targets/{releaseRuleTargetId}` | `ReleaseRuleService/DeleteReleaseRuleTarget` | `lms.release_rule_target.delete` |  | Delete ReleaseRuleTarget. |
| 1337 | `GET /v1/lms/content-items/{contentItemId}/release-grants` | `ContentItemService/ListReleaseGrants` | `lms.release_grant.read` |  | List ReleaseGrants. |
| 1338 | `GET /v1/lms/content-items/{contentItemId}/release-grants/{releaseGrantId}` | `ContentItemService/GetReleaseGrant` | `lms.release_grant.read` |  | Get ReleaseGrant. |
| 1339 | `DELETE /v1/lms/content-items/{contentItemId}/release-grants/{releaseGrantId}` | `ContentItemService/DeleteReleaseGrant` | `lms.release_grant.delete` |  | Delete ReleaseGrant. |
| 1340 | `GET /v1/lms/student-alerts` | `StudentAlertService/ListStudentAlerts` | `lms.student_alert.read` |  | List StudentAlerts. |
| 1341 | `POST /v1/lms/student-alerts` | `StudentAlertService/CreateStudentAlert` | `lms.student_alert.create` |  | Create StudentAlert. |
| 1342 | `GET /v1/lms/student-alerts/{studentAlertId}` | `StudentAlertService/GetStudentAlert` | `lms.student_alert.read` |  | Get StudentAlert. |
| 1343 | `PATCH /v1/lms/student-alerts/{studentAlertId}` | `StudentAlertService/UpdateStudentAlert` | `lms.student_alert.update` |  | Update StudentAlert. |
| 1344 | `DELETE /v1/lms/student-alerts/{studentAlertId}` | `StudentAlertService/DeleteStudentAlert` | `lms.student_alert.delete` |  | Delete StudentAlert. |
| 1345 | `GET /v1/lms/content-view-sessions` | `ContentViewSessionService/ListContentViewSessions` | `lms.content_view_session.read` |  | List ContentViewSessions. |
| 1346 | `POST /v1/lms/content-view-sessions` | `ContentViewSessionService/CreateContentViewSession` | `lms.content_view_session.create` |  | Create ContentViewSession. |
| 1347 | `GET /v1/lms/content-view-sessions/{contentViewSessionId}` | `ContentViewSessionService/GetContentViewSession` | `lms.content_view_session.read` |  | Get ContentViewSession. |
| 1348 | `PATCH /v1/lms/content-view-sessions/{contentViewSessionId}` | `ContentViewSessionService/UpdateContentViewSession` | `lms.content_view_session.update` |  | Update ContentViewSession. |
| 1349 | `GET /v1/lms/bookmarks` | `BookmarkService/ListBookmarks` | `lms.bookmark.read` |  | List Bookmarks. |
| 1350 | `POST /v1/lms/bookmarks` | `BookmarkService/CreateBookmark` | `lms.bookmark.create` |  | Create Bookmark. |
| 1351 | `GET /v1/lms/bookmarks/{bookmarkId}` | `BookmarkService/GetBookmark` | `lms.bookmark.read` |  | Get Bookmark. |
| 1352 | `PATCH /v1/lms/bookmarks/{bookmarkId}` | `BookmarkService/UpdateBookmark` | `lms.bookmark.update` |  | Update Bookmark. |
| 1353 | `DELETE /v1/lms/bookmarks/{bookmarkId}` | `BookmarkService/DeleteBookmark` | `lms.bookmark.delete` |  | Delete Bookmark. |
| 1354 | `GET /v1/lms/mentorships` | `MentorshipService/ListMentorships` | `lms.mentorship.read` |  | List Mentorships. |
| 1355 | `POST /v1/lms/mentorships` | `MentorshipService/CreateMentorship` | `lms.mentorship.create` |  | Create Mentorship. |
| 1356 | `GET /v1/lms/mentorships/{mentorshipId}` | `MentorshipService/GetMentorship` | `lms.mentorship.read` |  | Get Mentorship. |
| 1357 | `PATCH /v1/lms/mentorships/{mentorshipId}` | `MentorshipService/UpdateMentorship` | `lms.mentorship.update` |  | Update Mentorship. |
| 1358 | `DELETE /v1/lms/mentorships/{mentorshipId}` | `MentorshipService/DeleteMentorship` | `lms.mentorship.delete` |  | Delete Mentorship. |
| 1359 | `GET /v1/lms/plagiarism-accounts` | `PlagiarismAccountService/ListPlagiarismAccounts` | `lms.plagiarism_account.read` |  | List PlagiarismAccounts. |
| 1360 | `POST /v1/lms/plagiarism-accounts` | `PlagiarismAccountService/CreatePlagiarismAccount` | `lms.plagiarism_account.create` |  | Create PlagiarismAccount. |
| 1361 | `GET /v1/lms/plagiarism-accounts/{plagiarismAccountId}` | `PlagiarismAccountService/GetPlagiarismAccount` | `lms.plagiarism_account.read` |  | Get PlagiarismAccount. |
| 1362 | `PATCH /v1/lms/plagiarism-accounts/{plagiarismAccountId}` | `PlagiarismAccountService/UpdatePlagiarismAccount` | `lms.plagiarism_account.update` |  | Update PlagiarismAccount. |
| 1363 | `DELETE /v1/lms/plagiarism-accounts/{plagiarismAccountId}` | `PlagiarismAccountService/DeletePlagiarismAccount` | `lms.plagiarism_account.delete` |  | Delete PlagiarismAccount. |
| 1364 | `GET /v1/lms/cohorts/{cohortId}/plagiarism-links` | `CohortService/ListPlagiarismCohortLinks` | `lms.plagiarism_cohort_link.read` |  | List PlagiarismCohortLinks. |
| 1365 | `POST /v1/lms/cohorts/{cohortId}/plagiarism-links` | `CohortService/CreatePlagiarismCohortLink` | `lms.plagiarism_cohort_link.create` |  | Create PlagiarismCohortLink. |
| 1366 | `GET /v1/lms/cohorts/{cohortId}/plagiarism-links/{plagiarismCohortLinkId}` | `CohortService/GetPlagiarismCohortLink` | `lms.plagiarism_cohort_link.read` |  | Get PlagiarismCohortLink. |
| 1367 | `PATCH /v1/lms/cohorts/{cohortId}/plagiarism-links/{plagiarismCohortLinkId}` | `CohortService/UpdatePlagiarismCohortLink` | `lms.plagiarism_cohort_link.update` |  | Update PlagiarismCohortLink. |
| 1368 | `DELETE /v1/lms/cohorts/{cohortId}/plagiarism-links/{plagiarismCohortLinkId}` | `CohortService/DeletePlagiarismCohortLink` | `lms.plagiarism_cohort_link.delete` |  | Delete PlagiarismCohortLink. |
| 1369 | `GET /v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links` | `PlagiarismAccountService/ListPlagiarismUserLinks` | `lms.plagiarism_user_link.read` |  | List PlagiarismUserLinks. |
| 1370 | `POST /v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links` | `PlagiarismAccountService/CreatePlagiarismUserLink` | `lms.plagiarism_user_link.create` |  | Create PlagiarismUserLink. |
| 1371 | `GET /v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links/{plagiarismUserLinkId}` | `PlagiarismAccountService/GetPlagiarismUserLink` | `lms.plagiarism_user_link.read` |  | Get PlagiarismUserLink. |
| 1372 | `PATCH /v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links/{plagiarismUserLinkId}` | `PlagiarismAccountService/UpdatePlagiarismUserLink` | `lms.plagiarism_user_link.update` |  | Update PlagiarismUserLink. |
| 1373 | `DELETE /v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links/{plagiarismUserLinkId}` | `PlagiarismAccountService/DeletePlagiarismUserLink` | `lms.plagiarism_user_link.delete` |  | Delete PlagiarismUserLink. |
| 1374 | `GET /v1/lms/plagiarism-reports` | `PlagiarismReportService/ListPlagiarismReports` | `lms.plagiarism_report.read` |  | List PlagiarismReports. |
| 1375 | `GET /v1/lms/plagiarism-reports/{plagiarismReportId}` | `PlagiarismReportService/GetPlagiarismReport` | `lms.plagiarism_report.read` |  | Get PlagiarismReport. |
| 1376 | `GET /v1/lms/quizzes/{quizId}/question-history` | `QuizService/ListQuizQuestionHistory` | `lms.quiz_question_history.read` |  | List QuizQuestionHistory. |
| 1377 | `GET /v1/lms/quizzes/{quizId}/question-history/{quizQuestionHistoryId}` | `QuizService/GetQuizQuestionHistory` | `lms.quiz_question_history.read` |  | Get QuizQuestionHistory. |
| 1378 | `GET /v1/lms/questions/{questionId}/blanks` | `QuestionService/ListQuestionBlanks` | `lms.question_blank.read` |  | List QuestionBlanks. |
| 1379 | `POST /v1/lms/questions/{questionId}/blanks` | `QuestionService/CreateQuestionBlank` | `lms.question_blank.create` |  | Create QuestionBlank. |
| 1380 | `GET /v1/lms/questions/{questionId}/blanks/{questionBlankId}` | `QuestionService/GetQuestionBlank` | `lms.question_blank.read` |  | Get QuestionBlank. |
| 1381 | `PATCH /v1/lms/questions/{questionId}/blanks/{questionBlankId}` | `QuestionService/UpdateQuestionBlank` | `lms.question_blank.update` |  | Update QuestionBlank. |
| 1382 | `DELETE /v1/lms/questions/{questionId}/blanks/{questionBlankId}` | `QuestionService/DeleteQuestionBlank` | `lms.question_blank.delete` |  | Delete QuestionBlank. |
| 1383 | `GET /v1/lms/question-blank-answers` | `QuestionBlankAnswerService/ListQuestionBlankAnswers` | `lms.question_blank_answer.read` |  | List QuestionBlankAnswers. |
| 1384 | `POST /v1/lms/question-blank-answers` | `QuestionBlankAnswerService/CreateQuestionBlankAnswer` | `lms.question_blank_answer.create` |  | Create QuestionBlankAnswer. |
| 1385 | `GET /v1/lms/question-blank-answers/{questionBlankAnswerId}` | `QuestionBlankAnswerService/GetQuestionBlankAnswer` | `lms.question_blank_answer.read` |  | Get QuestionBlankAnswer. |
| 1386 | `PATCH /v1/lms/question-blank-answers/{questionBlankAnswerId}` | `QuestionBlankAnswerService/UpdateQuestionBlankAnswer` | `lms.question_blank_answer.update` |  | Update QuestionBlankAnswer. |
| 1387 | `DELETE /v1/lms/question-blank-answers/{questionBlankAnswerId}` | `QuestionBlankAnswerService/DeleteQuestionBlankAnswer` | `lms.question_blank_answer.delete` |  | Delete QuestionBlankAnswer. |
| 1388 | `GET /v1/lms/questions/{questionId}/hotspots` | `QuestionService/ListQuestionHotspots` | `lms.question_hotspot.read` |  | List QuestionHotspots. |
| 1389 | `POST /v1/lms/questions/{questionId}/hotspots` | `QuestionService/CreateQuestionHotspot` | `lms.question_hotspot.create` |  | Create QuestionHotspot. |
| 1390 | `GET /v1/lms/questions/{questionId}/hotspots/{questionHotspotId}` | `QuestionService/GetQuestionHotspot` | `lms.question_hotspot.read` |  | Get QuestionHotspot. |
| 1391 | `PATCH /v1/lms/questions/{questionId}/hotspots/{questionHotspotId}` | `QuestionService/UpdateQuestionHotspot` | `lms.question_hotspot.update` |  | Update QuestionHotspot. |
| 1392 | `DELETE /v1/lms/questions/{questionId}/hotspots/{questionHotspotId}` | `QuestionService/DeleteQuestionHotspot` | `lms.question_hotspot.delete` |  | Delete QuestionHotspot. |
| 1393 | `GET /v1/lms/question-hotspot-regions` | `QuestionHotspotRegionService/ListQuestionHotspotRegions` | `lms.question_hotspot_region.read` |  | List QuestionHotspotRegions. |
| 1394 | `POST /v1/lms/question-hotspot-regions` | `QuestionHotspotRegionService/CreateQuestionHotspotRegion` | `lms.question_hotspot_region.create` |  | Create QuestionHotspotRegion. |
| 1395 | `GET /v1/lms/question-hotspot-regions/{questionHotspotRegionId}` | `QuestionHotspotRegionService/GetQuestionHotspotRegion` | `lms.question_hotspot_region.read` |  | Get QuestionHotspotRegion. |
| 1396 | `PATCH /v1/lms/question-hotspot-regions/{questionHotspotRegionId}` | `QuestionHotspotRegionService/UpdateQuestionHotspotRegion` | `lms.question_hotspot_region.update` |  | Update QuestionHotspotRegion. |
| 1397 | `DELETE /v1/lms/question-hotspot-regions/{questionHotspotRegionId}` | `QuestionHotspotRegionService/DeleteQuestionHotspotRegion` | `lms.question_hotspot_region.delete` |  | Delete QuestionHotspotRegion. |
| 1398 | `GET /v1/lms/questions/{questionId}/match-pairs` | `QuestionService/ListQuestionMatchPairs` | `lms.question_match_pair.read` |  | List QuestionMatchPairs. |
| 1399 | `POST /v1/lms/questions/{questionId}/match-pairs` | `QuestionService/CreateQuestionMatchPair` | `lms.question_match_pair.create` |  | Create QuestionMatchPair. |
| 1400 | `GET /v1/lms/questions/{questionId}/match-pairs/{questionMatchPairId}` | `QuestionService/GetQuestionMatchPair` | `lms.question_match_pair.read` |  | Get QuestionMatchPair. |
| 1401 | `PATCH /v1/lms/questions/{questionId}/match-pairs/{questionMatchPairId}` | `QuestionService/UpdateQuestionMatchPair` | `lms.question_match_pair.update` |  | Update QuestionMatchPair. |
| 1402 | `DELETE /v1/lms/questions/{questionId}/match-pairs/{questionMatchPairId}` | `QuestionService/DeleteQuestionMatchPair` | `lms.question_match_pair.delete` |  | Delete QuestionMatchPair. |
| 1403 | `GET /v1/lms/questions/{questionId}/patterns` | `QuestionService/ListQuestionPatterns` | `lms.question_pattern.read` |  | List QuestionPatterns. |
| 1404 | `POST /v1/lms/questions/{questionId}/patterns` | `QuestionService/CreateQuestionPattern` | `lms.question_pattern.create` |  | Create QuestionPattern. |
| 1405 | `GET /v1/lms/questions/{questionId}/patterns/{questionPatternId}` | `QuestionService/GetQuestionPattern` | `lms.question_pattern.read` |  | Get QuestionPattern. |
| 1406 | `PATCH /v1/lms/questions/{questionId}/patterns/{questionPatternId}` | `QuestionService/UpdateQuestionPattern` | `lms.question_pattern.update` |  | Update QuestionPattern. |
| 1407 | `DELETE /v1/lms/questions/{questionId}/patterns/{questionPatternId}` | `QuestionService/DeleteQuestionPattern` | `lms.question_pattern.delete` |  | Delete QuestionPattern. |
| 1408 | `GET /v1/lms/question-pools` | `QuestionPoolService/ListQuestionPools` | `lms.question_pool.read` |  | List QuestionPools. |
| 1409 | `POST /v1/lms/question-pools` | `QuestionPoolService/CreateQuestionPool` | `lms.question_pool.create` |  | Create QuestionPool. |
| 1410 | `GET /v1/lms/question-pools/{questionPoolId}` | `QuestionPoolService/GetQuestionPool` | `lms.question_pool.read` |  | Get QuestionPool. |
| 1411 | `PATCH /v1/lms/question-pools/{questionPoolId}` | `QuestionPoolService/UpdateQuestionPool` | `lms.question_pool.update` |  | Update QuestionPool. |
| 1412 | `DELETE /v1/lms/question-pools/{questionPoolId}` | `QuestionPoolService/DeleteQuestionPool` | `lms.question_pool.delete` |  | Delete QuestionPool. |
| 1413 | `GET /v1/lms/question-pools/{questionPoolId}/items` | `QuestionPoolService/ListQuestionPoolItems` | `lms.question_pool_item.read` |  | List QuestionPoolItems. |
| 1414 | `POST /v1/lms/question-pools/{questionPoolId}/items` | `QuestionPoolService/CreateQuestionPoolItem` | `lms.question_pool_item.create` |  | Create QuestionPoolItem. |
| 1415 | `GET /v1/lms/question-pools/{questionPoolId}/items/{questionPoolItemId}` | `QuestionPoolService/GetQuestionPoolItem` | `lms.question_pool_item.read` |  | Get QuestionPoolItem. |
| 1416 | `PATCH /v1/lms/question-pools/{questionPoolId}/items/{questionPoolItemId}` | `QuestionPoolService/UpdateQuestionPoolItem` | `lms.question_pool_item.update` |  | Update QuestionPoolItem. |
| 1417 | `DELETE /v1/lms/question-pools/{questionPoolId}/items/{questionPoolItemId}` | `QuestionPoolService/DeleteQuestionPoolItem` | `lms.question_pool_item.delete` |  | Delete QuestionPoolItem. |
| 1418 | `POST /v1/lms/courses/{courseId}:publish` | `LmsCourseService/PublishCourse` | `lms.course.publish_course` |  | Publish. |
| 1419 | `POST /v1/lms/courses/{courseId}:unpublish` | `LmsCourseService/UnpublishCourse` | `lms.course.unpublish_course` |  | Unpublish. |
| 1420 | `POST /v1/lms/courses/{courseId}:submitForReview` | `LmsCourseService/SubmitCourseForReview` | `lms.course.submit_course_for_review` |  | Send to review. |
| 1421 | `POST /v1/lms/courses/{courseId}:approve` | `LmsCourseService/ApproveCourse` | `lms.course.approve_course` |  | Approve. |
| 1422 | `GET /v1/lms/courses/{courseId}:outline` | `LmsCourseService/GetCourseOutline` | `lms.course.get_course_outline` | Public | Chapters and lessons of a published course, with the caller's progress when signed in. |
| 1423 | `POST /v1/lms/courses/{courseId}:reorderChapters` | `LmsCourseService/ReorderChapters` | `lms.course.reorder_chapters` |  | Reorder chapters. |
| 1424 | `POST /v1/lms/courses/{courseId}:enroll` | `LmsCourseService/EnrollInCourse` | `lms.course.enroll_in_course` |  | Self-enrol the caller. |
| 1425 | `POST /v1/lms/courses/{courseId}:batchEnroll` | `LmsCourseService/BatchEnrollInCourse` | `lms.course.batch_enroll_in_course` |  | Enrol many students (or preview). |
| 1426 | `POST /v1/lms/courses/{courseId}:issueMyCertificate` | `LmsCourseService/IssueMyCertificate` | `lms.course.issue_my_certificate` |  | Issue a certificate to the caller when the course is complete and certification is enabled. |
| 1427 | `GET /v1/lms/courses/{courseId}:progressDistribution` | `LmsCourseService/GetCourseProgressDistribution` | `lms.course.get_course_progress_distribution` |  | Students per progress range. |
| 1428 | `GET /v1/lms/courses/{courseId}:evaluatorSlots` | `LmsCourseService/GetEvaluatorSlots` | `lms.course.get_evaluator_slots` |  | Free evaluation slots. |
| 1429 | `POST /v1/lms/courses/{courseId}:export` | `LmsCourseService/ExportCourse` | `lms.course.export_course` |  | Export the course as a package (zip). |
| 1430 | `POST /v1/lms/courses/{courseId}:importPackage` | `LmsCourseService/ImportCoursePackage` | `lms.course.import_course_package` |  | Import a course package (zip) into the course. |
| 1431 | `GET /v1/lms/chapters/{chapterId}:scormLaunch` | `LmsCourseService/GetScormLaunch` | `lms.chapter.get_scorm_launch` |  | Launch URL and saved runtime state of a SCORM chapter. |
| 1432 | `POST /v1/lms/chapters/{chapterId}:saveScormProgress` | `LmsCourseService/SaveScormProgress` | `lms.chapter.save_scorm_progress` |  | Persist SCORM runtime data and completion. |
| 1433 | `GET /v1/lms/lessons/{lessonId}:content` | `LessonService/GetLessonContent` | `lms.lesson.get_lesson_content` | Public | Lesson body with navigation. |
| 1434 | `POST /v1/lms/lessons/{lessonId}:move` | `LessonService/MoveLesson` | `lms.lesson.move_lesson` |  | Move / reorder a lesson. |
| 1435 | `POST /v1/lms/lessons/{lessonId}:markComplete` | `LessonService/MarkLessonComplete` | `lms.lesson.mark_lesson_complete` |  | Record completion for the caller (enforces video / quiz rules from settings). |
| 1436 | `POST /v1/lms/lessons/{lessonId}:trackVideoWatch` | `LessonService/TrackVideoWatch` | `lms.lesson.track_video_watch` |  | Record video watch time. |
| 1437 | `POST /v1/lms/content-items/{contentItemId}:startQuizAttempt` | `QuizService/StartQuizAttempt` | `lms.quiz.start_quiz_attempt` |  | Start a quiz attempt on its placement. |
| 1438 | `POST /v1/lms/quizzes/{quizId}:checkAnswer` | `QuizService/CheckQuizAnswer` | `lms.quiz.check_quiz_answer` |  | Instant feedback for practice quizzes. |
| 1439 | `POST /v1/lms/quiz-submissions/{quizSubmissionId}:submit` | `QuizService/SubmitQuiz` | `lms.quiz.submit_quiz` |  | Grade and store the attempt. |
| 1440 | `GET /v1/lms/questions:search` | `QuestionService/SearchQuestionBank` | `lms.question.search_question_bank` |  | Find questions to add to a quiz. |
| 1441 | `POST /v1/lms/assignment-submissions/{assignmentSubmissionId}:grade` | `LmsAssignmentSubmissionService/GradeLearningSubmission` | `lms.assignment_submission.grade_learning_submission` |  | Evaluate a learning-assignment submission. |
| 1442 | `POST /v1/lms/programming-exercises/{programmingExerciseId}:submit` | `ProgrammingExerciseService/SubmitExercise` | `lms.programming_exercise.submit_exercise` |  | Store a submission and evaluate it against the test cases. |
| 1443 | `POST /v1/lms/cohorts/{cohortId}:publish` | `CohortService/PublishCohort` | `lms.cohort.publish_cohort` |  | Publish. |
| 1444 | `POST /v1/lms/cohorts/{cohortId}:unpublish` | `CohortService/UnpublishCohort` | `lms.cohort.unpublish_cohort` |  | Unpublish. |
| 1445 | `POST /v1/lms/cohorts/{cohortId}:enroll` | `CohortService/EnrollInCohort` | `lms.cohort.enroll_in_cohort` |  | Self-enrol. |
| 1446 | `POST /v1/lms/cohorts/{cohortId}:batchEnroll` | `CohortService/BatchEnrollInCohort` | `lms.cohort.batch_enroll_in_cohort` |  | Enrol many students. |
| 1447 | `GET /v1/lms/cohorts/{cohortId}:timetable` | `CohortService/GetCohortTimetable` | `lms.cohort.get_cohort_timetable` |  | Timetable with legend. |
| 1448 | `POST /v1/lms/cohorts/{cohortId}:applyTimetableTemplate` | `CohortService/ApplyTimetableTemplate` | `lms.cohort.apply_timetable_template` |  | Materialise a template into dated entries. |
| 1449 | `POST /v1/lms/cohorts/{cohortId}:sendAnnouncement` | `CohortService/SendCohortAnnouncement` | `lms.cohort.send_cohort_announcement` |  | E-mail all students. |
| 1450 | `GET /v1/lms/cohorts/{cohortId}:progress` | `CohortService/GetCohortProgress` | `lms.cohort.get_cohort_progress` |  | Student progress across courses and assessments. |
| 1451 | `POST /v1/lms/cohorts/{cohortId}:generateCertificates` | `CohortService/GenerateCohortCertificates` | `lms.cohort.generate_cohort_certificates` |  | Issue certificates to completing students. |
| 1452 | `POST /v1/lms/live-classes:schedule` | `LiveClassService/ScheduleLiveClass` | `lms.live_class.schedule_live_class` |  | Create the meeting with the provider, the calendar event and invites. |
| 1453 | `GET /v1/lms/live-classes/{liveClassId}:startUrl` | `LiveClassService/GetLiveClassStartUrl` | `lms.live_class.get_live_class_start_url` |  | Fresh host start URL. |
| 1454 | `POST /v1/lms/live-classes/{liveClassId}:join` | `LiveClassService/JoinLiveClass` | `lms.live_class.join_live_class` |  | Participant join URL (records intent). |
| 1455 | `POST /v1/lms/live-classes/{liveClassId}:retryInvites` | `LiveClassService/RetryLiveClassInvites` | `lms.live_class.retry_live_class_invites` |  | Resend calendar invites. |
| 1456 | `POST /v1/lms/integrations/{provider}:webhook` | `ConferencingAccountService/ReceiveConferencingWebhook` | `lms.webhook.receive` | Public | Provider callback (meeting ended, participant joined/left). |
| 1457 | `POST /v1/lms/evaluators/{evaluatorId}:connectCalendar` | `EvaluatorService/ConnectEvaluatorCalendar` | `lms.evaluator.connect_evaluator_calendar` |  | Start OAuth to link the evaluator's calendar. |
| 1458 | `POST /v1/lms/evaluators/{evaluatorId}:disconnectCalendar` | `EvaluatorService/DisconnectEvaluatorCalendar` | `lms.evaluator.disconnect_evaluator_calendar` |  | Unlink the calendar. |
| 1459 | `POST /v1/lms/certificate-requests/{certificateRequestId}:cancel` | `CertificateRequestService/CancelCertificateRequest` | `lms.certificate_request.cancel_certificate_request` |  | Cancel the booking. |
| 1460 | `POST /v1/lms/certificate-requests/{certificateRequestId}:startEvaluation` | `CertificateRequestService/StartEvaluation` | `lms.certificate_request.start_evaluation` |  | Create the evaluation for a booked slot. |
| 1461 | `POST /v1/lms/certificate-evaluations/{certificateEvaluationId}:issueCertificate` | `CertificateEvaluationService/IssueCertificateFromEvaluation` | `lms.certificate_evaluation.issue_certificate_from_evaluation` |  | Issue a certificate for a passed evaluation. |
| 1462 | `GET /v1/lms/certificates/{certificateId}:verify` | `CertificateService/VerifyCertificate` | `lms.certificate.verify_certificate` | Public | Public verification of a certificate. |
| 1463 | `GET /v1/lms/certificates/{certificateId}:download` | `CertificateService/DownloadCertificate` | `lms.certificate.download_certificate` |  | PDF download. |
| 1464 | `POST /v1/lms/badges/{badgeId}:award` | `BadgeService/AwardBadge` | `lms.badge.award_badge` |  | Manually award a badge. |
| 1465 | `POST /v1/lms/learning-programs/{learningProgramId}:enroll` | `LearningProgramService/EnrollInLearningProgram` | `lms.learning_program.enroll_in_learning_program` |  | Join a program. |
| 1466 | `GET /v1/lms/me/dashboard` | `EnrollmentService/GetStudentDashboard` | `lms.enrollment.get_student_dashboard` |  | My courses, cohorts, resume point, live classes, streak and heatmap. |
| 1467 | `GET /v1/lms/statistics` | `EnrollmentService/GetLmsStatistics` | `lms.enrollment.get_lms_statistics` | Public | Public catalogue statistics (courses, students, certificates, completion). |
| 1468 | `GET /v1/lms/public-settings` | `SettingsService/GetPublicSettings` | `lms.settings.get_public_settings` | Public | Settings needed by the public catalogue (navigation, feature switches, contact, SEO). |
| 1469 | `POST /v1/lms/contact` | `SettingsService/SendContactMessage` | `lms.settings.send_contact_message` |  | Send a message to the LMS contact address configured in settings. |
| 1470 | `GET /v1/lms/members/{userId}:profile` | `EnrollmentService/GetMemberProfile` | `lms.enrollment.get_member_profile` |  | A member's profile for signed-in callers holding the permission: published certificates and enrolments within the caller's data scope, and badges (not public since 2026-11-17: lms keeps no names and cannot honour hideStudentInfo for a guest). |
| 1471 | `POST /v1/lms/courses/{courseId}:createVersion` | `LmsCourseService/CreateCourseVersion` | `lms.course.create_course_version` |  | Create the next version of a course (copies chapters, items, objectives, grading categories and release rules; previous_version_id and root_course_id are set). |
| 1472 | `POST /v1/lms/courses/{courseId}:deactivate` | `LmsCourseService/DeactivateCourse` | `lms.course.deactivate_course` |  | Deactivate a course version (no new cohorts). |
| 1473 | `POST /v1/lms/courses/{courseId}:reactivate` | `LmsCourseService/ReactivateCourse` | `lms.course.reactivate_course` |  | Reactivate a deactivated version. |
| 1474 | `POST /v1/lms/courses/{courseId}:publishCatalogListing` | `LmsCourseService/PublishCatalogListing` | `lms.course.publish_catalog_listing` |  | List the course in the catalogue. |
| 1475 | `POST /v1/lms/courses/{courseId}:hideCatalogListing` | `LmsCourseService/HideCatalogListing` | `lms.course.hide_catalog_listing` |  | Remove the course from the catalogue. |
| 1476 | `GET /v1/lms/courses/{courseId}:dependencies` | `LmsCourseService/CheckCourseDependencies` | `lms.course.check_course_dependencies` |  | What prevents deleting the course (cohorts, newer versions, catalogue listing). |
| 1477 | `GET /v1/lms/courses/{courseId}:syllabus` | `LmsCourseService/GetCourseSyllabus` | `lms.course.get_course_syllabus` |  | Syllabus document generated from the outline, objectives and grading categories. |
| 1478 | `GET /v1/lms/catalog` | `LmsCourseService/GetCatalogHome` | `lms.course.get_catalog_home` | Public | Catalogue home: banners, published courses, categories and cohorts open for enrolment. |
| 1479 | `GET /v1/lms/catalog:compare` | `LmsCourseService/CompareCourses` | `lms.course.compare_courses` | Public | Side-by-side catalogue details of several courses. |
| 1480 | `POST /v1/lms/chapters/{chapterId}:move` | `LmsCourseService/MoveChapter` | `lms.chapter.move_chapter` |  | Move / reorder a chapter (folder) inside its course or cohort. |
| 1481 | `POST /v1/lms/content-items/{contentItemId}:move` | `ContentItemService/MoveContentItem` | `lms.content_item.move_content_item` |  | Move / reorder an item. |
| 1482 | `POST /v1/lms/content-items/{contentItemId}:copy` | `ContentItemService/CopyContentItem` | `lms.content_item.copy_content_item` |  | Duplicate an item (and its underlying assignment / quiz / resource when it is not shared). |
| 1483 | `POST /v1/lms/content-items:addFromRepository` | `ContentItemService/AddFromRepository` | `lms.content_item.add_from_repository` |  | Place shared repository items in a chapter. |
| 1484 | `POST /v1/lms/content-items/{contentItemId}:setVisibility` | `ContentItemService/SetContentItemVisibility` | `lms.content_item.set_content_item_visibility` |  | Show or hide a cohort item. |
| 1485 | `GET /v1/lms/content-items/{contentItemId}:launch` | `ContentItemService/GetWebResourceLaunch` | `lms.content_item.get_web_resource_launch` |  | Launch URL of a web resource, e-book or file item (signed, short-lived). |
| 1486 | `POST /v1/lms/cohorts/{cohortId}:activate` | `CohortService/ActivateCohort` | `lms.cohort.activate_cohort` |  | Open the cohort to its members. |
| 1487 | `POST /v1/lms/cohorts/{cohortId}:deactivate` | `CohortService/DeactivateCohort` | `lms.cohort.deactivate_cohort` |  | Suspend the cohort. |
| 1488 | `POST /v1/lms/cohorts/{cohortId}:archive` | `CohortService/ArchiveCohort` | `lms.cohort.archive_cohort` |  | Archive a finished cohort (read-only; archive package kept). |
| 1489 | `POST /v1/lms/cohorts/{cohortId}:copyCourseOutline` | `CohortService/CopyCourseOutlineToCohort` | `lms.cohort.copy_course_outline_to_cohort` |  | Copy the course template (chapters, items, objectives, grading categories, release rules) into the cohort, converting day offsets into dates. |
| 1490 | `GET /v1/lms/cohorts/{cohortId}:roster` | `CohortService/GetCohortRoster` | `lms.cohort.get_cohort_roster` |  | Students and staff of a cohort. |
| 1491 | `POST /v1/lms/cohorts/{cohortId}:setMembers` | `CohortService/SetCohortMembers` | `lms.cohort.set_cohort_members` |  | Add, update or remove students and staff in one call. |
| 1492 | `POST /v1/lms/cohorts/{cohortId}:importMembers` | `CohortService/ImportCohortMembers` | `lms.cohort.import_cohort_members` |  | Bulk-add members from a file (background operation with row errors). |
| 1493 | `POST /v1/lms/cohorts/{cohortId}:setStudentAccess` | `CohortService/SetStudentAccess` | `lms.cohort.set_student_access` |  | Change the students' access window (e.g. |
| 1494 | `GET /v1/lms/cohorts/{cohortId}:syllabus` | `CohortService/GetCohortSyllabus` | `lms.cohort.get_cohort_syllabus` |  | Syllabus of the cohort with its dates. |
| 1495 | `GET /v1/lms/cohorts/{cohortId}:gradebook` | `CohortService/GetGradebook` | `lms.cohort.get_gradebook` |  | Gradebook grid: graded items × students with category and weighted totals. |
| 1496 | `GET /v1/lms/cohorts/{cohortId}:performance` | `CohortService/GetCohortPerformance` | `lms.cohort.get_cohort_performance` |  | Per-item statistics (average, median, missing). |
| 1497 | `POST /v1/lms/cohorts/{cohortId}:exportGrades` | `CohortService/ExportGrades` | `lms.cohort.export_grades` |  | Export grades (whole gradebook, or one assignment / quiz / discussion / participation item). |
| 1498 | `POST /v1/lms/cohorts/{cohortId}:publishFinalGrades` | `CohortService/PublishFinalGrades` | `lms.cohort.publish_final_grades` |  | Compute (from the gradebook and the cohort grading scheme) and publish final grades. |
| 1499 | `POST /v1/lms/cohorts/{cohortId}:evaluateReleaseRules` | `CohortService/EvaluateReleaseRules` | `lms.cohort.evaluate_release_rules` |  | Re-evaluate conditional-release rules (normally automatic after each grade). |
| 1500 | `POST /v1/lms/gradebook-entries:grade` | `GradebookEntryService/GradeEntry` | `lms.gradebook_entry.grade_entry` |  | Create or update the grade of a student / group for an item; records grade history and rubric choices. |
| 1501 | `POST /v1/lms/content-items/{contentItemId}:publishGrades` | `ContentItemService/PublishItemGrades` | `lms.content_item.publish_item_grades` |  | Publish grades of an item to students. |
| 1502 | `POST /v1/lms/content-items/{contentItemId}:reassign` | `ContentItemService/ReassignItem` | `lms.content_item.reassign_item` |  | Give students / groups a fresh attempt (keeps history in the reassignment log). |
| 1503 | `POST /v1/lms/content-items/{contentItemId}:downloadSubmissions` | `ContentItemService/DownloadSubmissions` | `lms.content_item.download_submissions` |  | Zip of all submitted files of an assignment. |
| 1504 | `GET /v1/lms/content-items/{contentItemId}:releaseStatus` | `ContentItemService/GetReleaseStatus` | `lms.content_item.get_release_status` |  | Whether the item is released to a student and which conditions are pending. |
| 1505 | `POST /v1/lms/content-items/{contentItemId}:grantAccess` | `ContentItemService/GrantItemAccess` | `lms.content_item.grant_item_access` |  | Manually release an item to students regardless of rules. |
| 1506 | `POST /v1/lms/content-items/{contentItemId}:setReleaseRulesEnabled` | `ContentItemService/SetReleaseRulesEnabled` | `lms.content_item.set_release_rules_enabled` |  | Enable / disable all release rules whose source is this item. |
| 1507 | `POST /v1/lms/content-items/{contentItemId}:reorderReleaseRules` | `ContentItemService/ReorderReleaseRules` | `lms.content_item.reorder_release_rules` |  | Change rule evaluation order. |
| 1508 | `POST /v1/lms/release-rules/{releaseRuleId}:enable` | `ReleaseRuleService/EnableReleaseRule` | `lms.release_rule.enable_release_rule` |  | Enable. |
| 1509 | `POST /v1/lms/release-rules/{releaseRuleId}:disable` | `ReleaseRuleService/DisableReleaseRule` | `lms.release_rule.disable_release_rule` |  | Disable. |
| 1510 | `POST /v1/lms/release-rules/{releaseRuleId}:copyTo` | `ReleaseRuleService/CopyReleaseRule` | `lms.release_rule.copy_release_rule` |  | Copy a rule (conditions and targets) to other items. |
| 1511 | `POST /v1/lms/assignment-submissions/{assignmentSubmissionId}:finalize` | `LmsAssignmentSubmissionService/FinalizeSubmission` | `lms.assignment_submission.finalize_submission` |  | Mark a (group) submission final; no further changes are accepted. |
| 1512 | `POST /v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:requestToJoin` | `ContentItemService/RequestToJoinGroup` | `lms.assignment_group.request_to_join_group` |  | Student asks to join a group (status requested). |
| 1513 | `POST /v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:setMembers` | `ContentItemService/SetGroupMembers` | `lms.assignment_group.set_group_members` |  | Add / remove members (teacher). |
| 1514 | `POST /v1/lms/assignment-group-members/{assignmentGroupMemberId}:accept` | `AssignmentGroupMemberService/AcceptGroupRequest` | `lms.assignment_group_member.accept_group_request` |  | Accept a join request. |
| 1515 | `POST /v1/lms/assignment-group-members/{assignmentGroupMemberId}:reject` | `AssignmentGroupMemberService/RejectGroupRequest` | `lms.assignment_group_member.reject_group_request` |  | Reject a join request. |
| 1516 | `POST /v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:submitPeerReview` | `ContentItemService/SubmitPeerReview` | `lms.assignment_group.submit_peer_review` |  | Student records peer-review points for the other members of their group. |
| 1517 | `POST /v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:finalizePeerReview` | `ContentItemService/FinalizePeerReview` | `lms.assignment_group.finalize_peer_review` |  | Teacher finalises peer points into the members' gradebook entries (peerPoints). |
| 1518 | `POST /v1/lms/plagiarism-reports:submit` | `PlagiarismReportService/SubmitForPlagiarismCheck` | `lms.plagiarism_report.submit_for_plagiarism_check` |  | Send a submitted file to the plagiarism provider. |
| 1519 | `GET /v1/lms/plagiarism-reports/{plagiarismReportId}:reportLink` | `PlagiarismReportService/GetPlagiarismReportLink` | `lms.plagiarism_report.get_plagiarism_report_link` |  | Signed link to the provider report viewer. |
| 1520 | `POST /v1/lms/integrations/plagiarism/{provider}:webhook` | `PlagiarismReportService/ReceivePlagiarismWebhook` | `lms.webhook.receive` | Public | Provider callback when a similarity report is ready. |
| 1521 | `POST /v1/lms/quiz-submissions/{quizSubmissionId}:saveResponses` | `QuizSubmissionService/SaveQuizResponses` | `lms.quiz_submission.save_quiz_responses` |  | Autosave answers of an attempt in progress. |
| 1522 | `POST /v1/lms/quiz-submissions/{quizSubmissionId}:pause` | `QuizSubmissionService/PauseQuizAttempt` | `lms.quiz_submission.pause_quiz_attempt` |  | Pause an attempt (quizzes that allow breaks). |
| 1523 | `POST /v1/lms/quiz-submissions/{quizSubmissionId}:resume` | `QuizSubmissionService/ResumeQuizAttempt` | `lms.quiz_submission.resume_quiz_attempt` |  | Resume a paused attempt. |
| 1524 | `POST /v1/lms/quiz-submissions/{quizSubmissionId}:gradeAnswers` | `QuizSubmissionService/GradeQuizAnswers` | `lms.quiz_submission.grade_quiz_answers` |  | Manually mark answers that need grading (open-ended, pattern...). |
| 1525 | `POST /v1/lms/quizzes/{quizId}:setQuestions` | `QuizService/SetQuizQuestions` | `lms.quiz.set_quiz_questions` |  | Add / remove quiz questions (earlier marks are kept in the question history when attempts exist). |
| 1526 | `POST /v1/lms/questions:import` | `QuestionService/ImportQuestions` | `lms.question.import_questions` |  | Import questions into the bank. |
| 1527 | `POST /v1/lms/forum-topics/{forumTopicId}:markRead` | `ForumTopicService/MarkTopicRead` | `lms.forum_topic.mark_topic_read` |  | Record read state for unread counts. |
| 1528 | `POST /v1/lms/forum-topics/{forumTopicId}:hide` | `ForumTopicService/HideForumTopic` | `lms.forum_topic.hide_forum_topic` |  | Hide from students. |
| 1529 | `POST /v1/lms/forum-topics/{forumTopicId}:unhide` | `ForumTopicService/UnhideForumTopic` | `lms.forum_topic.unhide_forum_topic` |  | Show again. |
| 1530 | `POST /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:hide` | `ForumTopicService/HideForumPost` | `lms.forum_post.hide_forum_post` |  | Hide a post (and its replies). |
| 1531 | `POST /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:unhide` | `ForumTopicService/UnhideForumPost` | `lms.forum_post.unhide_forum_post` |  | Show again. |
| 1532 | `POST /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:confirmAbuse` | `ForumTopicService/ConfirmPostAbuse` | `lms.forum_post.confirm_post_abuse` |  | Confirm reports and hide the post. |
| 1533 | `POST /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:flag` | `ForumTopicService/FlagForumPost` | `lms.forum_post.flag_forum_post` |  | Flag a post for yourself. |
| 1534 | `POST /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:markSubstantive` | `ForumTopicService/MarkPostSubstantive` | `lms.forum_post.mark_post_substantive` |  | Teacher marks a post substantive (counts for discussion grading). |
| 1535 | `GET /v1/lms/me/unread-counts` | `CohortService/GetUnreadCounts` | `lms.forum.get_unread_counts` |  | Unread posts, notifications and mail for badges in the apps. |
| 1536 | `POST /v1/lms/live-classes/{liveClassId}:end` | `LiveClassService/EndLiveClass` | `lms.live_class.end_live_class` |  | End the meeting at the provider for everyone. |
| 1537 | `POST /v1/lms/live-classes/{liveClassId}:syncAttendance` | `LiveClassService/SyncLiveClassAttendance` | `lms.live_class.sync_live_class_attendance` |  | Fetch attendance (join / leave times) from the provider. |
| 1538 | `POST /v1/lms/ebooks/{ebookId}:convert` | `EbookService/ConvertEbook` | `lms.ebook.convert_ebook` |  | Send the uploaded source document for conversion (state on the source file's processing_status). |
| 1539 | `POST /v1/lms/integrations/conversion/{provider}:webhook` | `EbookService/ReceiveEbookConversion` | `lms.webhook.receive` | Public | Conversion-service callback with the converted e-book. |
| 1540 | `POST /v1/lms/ebooks/{ebookId}:publishCuration` | `EbookService/PublishEbookCuration` | `lms.ebook.publish_ebook_curation` |  | Teacher curates the related materials shown with an e-book. |
| 1541 | `GET /v1/lms/ebooks/{ebookId}:myRelatedMaterials` | `EbookService/GetMyRelatedMaterials` | `lms.ebook.get_my_related_materials` |  | Related materials ranked for the caller. |
| 1542 | `POST /v1/lms/content-items/{contentItemId}:recordView` | `ContentItemService/RecordItemView` | `lms.content_item.record_item_view` |  | Count a view (and optionally a like) of an item. |
| 1543 | `POST /v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId}:launch` | `ExternalToolResourceService/LaunchExternalTool` | `lms.external_tool_link.launch_external_tool` |  | Signed LTI launch for the caller. |
| 1544 | `POST /v1/lms/integrations/lti:outcome` | `ExternalToolResourceService/ReceiveToolOutcome` | `lms.webhook.receive` | Public | LTI grade pass-back from a tool into the gradebook: LTI 1.1 Basic Outcomes (XML, OAuth 1.0a body signature) or LTI Advantage score (JSON, JWT bearer from the tool). |
| 1545 | `POST /v1/lms/external-tools/{externalToolId}:activate` | `ExternalToolService/ActivateExternalTool` | `lms.external_tool.activate_external_tool` |  | Validate a licence key and enable the tool for the tenant. |
| 1546 | `POST /v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}:approve` | `LmsCourseService/ApproveEnrollmentRequest` | `lms.enrollment_request.approve_enrollment_request` |  | Approve and enrol in a cohort. |
| 1547 | `POST /v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}:reject` | `LmsCourseService/RejectEnrollmentRequest` | `lms.enrollment_request.reject_enrollment_request` |  | Reject. |
| 1548 | `GET /v1/lms/me/teacher-dashboard` | `LmsCourseService/GetTeacherDashboard` | `lms.course.get_teacher_dashboard` |  | Teacher home: my cohorts, grading queue, unread posts, upcoming items and alerts. |
| 1549 | `POST /v1/lms/gradebook-entries:batchGrade` | `GradebookEntryService/BatchGradeEntries` | `lms.gradebook_entry.batch_grade_entries` |  | Grade many students in one call; each row succeeds or fails on its own (stale etags are reported per row). |
| 1550 | `GET /v1/lms/import-export-jobs` | `ImportExportJobService/ListImportExportJobs` | `lms.import_export_job.read` |  | List ImportExportJobs. |
| 1551 | `POST /v1/lms/import-export-jobs` | `ImportExportJobService/CreateImportExportJob` | `lms.import_export_job.create` |  | Create ImportExportJob. |
| 1552 | `GET /v1/lms/import-export-jobs/{importExportJobId}` | `ImportExportJobService/GetImportExportJob` | `lms.import_export_job.read` |  | Get ImportExportJob. |
| 1553 | `GET /v1/lms/access-logs` | `AccessLogService/ListAccessLogs` | `lms.access_log.read` |  | List AccessLogs. |
| 1554 | `GET /v1/lms/access-logs/{accessLogId}` | `AccessLogService/GetAccessLog` | `lms.access_log.read` |  | Get AccessLog. |
| 1555 | `GET /v1/lms/drive-items` | `DriveItemService/ListDriveItems` | `lms.drive_item.read` |  | List DriveItems. |
| 1556 | `POST /v1/lms/drive-items` | `DriveItemService/CreateDriveItem` | `lms.drive_item.create` |  | Create DriveItem. |
| 1557 | `GET /v1/lms/drive-items/{driveItemId}` | `DriveItemService/GetDriveItem` | `lms.drive_item.read` |  | Get DriveItem. |
| 1558 | `PATCH /v1/lms/drive-items/{driveItemId}` | `DriveItemService/UpdateDriveItem` | `lms.drive_item.update` |  | Update DriveItem. |
| 1559 | `DELETE /v1/lms/drive-items/{driveItemId}` | `DriveItemService/DeleteDriveItem` | `lms.drive_item.delete` |  | Delete DriveItem. |
| 1560 | `GET /v1/lms/drive-items/{driveItemId}/shares` | `DriveItemService/ListDriveItemShares` | `lms.drive_item_share.read` |  | List DriveItemShares. |
| 1561 | `POST /v1/lms/drive-items/{driveItemId}/shares` | `DriveItemService/CreateDriveItemShare` | `lms.drive_item_share.create` |  | Create DriveItemShare. |
| 1562 | `PATCH /v1/lms/drive-items/{driveItemId}/shares/{driveItemShareId}` | `DriveItemService/UpdateDriveItemShare` | `lms.drive_item_share.update` |  | Update DriveItemShare. |
| 1563 | `DELETE /v1/lms/drive-items/{driveItemId}/shares/{driveItemShareId}` | `DriveItemService/DeleteDriveItemShare` | `lms.drive_item_share.delete` |  | Delete DriveItemShare. |
| 1564 | `POST /v1/lms/import-export-jobs/{importExportJobId}:cancel` | `ImportExportJobService/CancelImportExportJob` | `lms.import_export_job.cancel_import_export_job` |  | Cancel a pending job. |
| 1565 | `POST /v1/lms/import-export-jobs/{importExportJobId}:restore` | `ImportExportJobService/RestoreCohortArchive` | `lms.import_export_job.restore_cohort_archive` |  | Restore an archived cohort from its export. |
| 1566 | `GET /v1/lms/import-export-jobs/{importExportJobId}:package` | `ImportExportJobService/GetJobPackage` | `lms.import_export_job.get_job_package` |  | Download the export package or the import error log. |
| 1567 | `POST /v1/lms/drive-items/{driveItemId}:share` | `DriveItemService/ShareDriveItem` | `lms.drive_item.share_drive_item` |  | Share a drive item with users. |
| 1568 | `GET /v1/lms/operations` | `OperationService/ListOperations` | `lms.operation.read` |  | Long-running operations of this module started by the caller. |
| 1569 | `GET /v1/lms/operations/{operationId}` | `OperationService/GetOperation` | `lms.operation.read` |  | Status of a long-running operation started in this module. |
| 1570 | `POST /v1/lms/operations/{operationId}:cancel` | `OperationService/CancelOperation` | `lms.operation.cancel_operation` |  | Request cancellation of a running operation. |

## Fees & Payments (`finance`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 1571 | `GET /v1/finance/fee-heads` | `FeeHeadService/ListFeeHeads` | `finance.fee_head.read` |  | List FeeHeads. |
| 1572 | `POST /v1/finance/fee-heads` | `FeeHeadService/CreateFeeHead` | `finance.fee_head.create` |  | Create FeeHead. |
| 1573 | `GET /v1/finance/fee-heads/{feeHeadId}` | `FeeHeadService/GetFeeHead` | `finance.fee_head.read` |  | Get FeeHead. |
| 1574 | `PATCH /v1/finance/fee-heads/{feeHeadId}` | `FeeHeadService/UpdateFeeHead` | `finance.fee_head.update` |  | Update FeeHead. |
| 1575 | `DELETE /v1/finance/fee-heads/{feeHeadId}` | `FeeHeadService/DeleteFeeHead` | `finance.fee_head.delete` |  | Delete FeeHead. |
| 1576 | `GET /v1/finance/fee-plans` | `FeePlanService/ListFeePlans` | `finance.fee_plan.read` |  | List FeePlans. |
| 1577 | `POST /v1/finance/fee-plans` | `FeePlanService/CreateFeePlan` | `finance.fee_plan.create` |  | Create FeePlan. |
| 1578 | `GET /v1/finance/fee-plans/{feePlanId}` | `FeePlanService/GetFeePlan` | `finance.fee_plan.read` |  | Get FeePlan. |
| 1579 | `PATCH /v1/finance/fee-plans/{feePlanId}` | `FeePlanService/UpdateFeePlan` | `finance.fee_plan.update` |  | Update FeePlan. |
| 1580 | `DELETE /v1/finance/fee-plans/{feePlanId}` | `FeePlanService/DeleteFeePlan` | `finance.fee_plan.delete` |  | Delete FeePlan. |
| 1581 | `GET /v1/finance/fee-plans/{feePlanId}/installments` | `FeePlanService/ListFeePlanInstallments` | `finance.fee_plan_installment.read` |  | List FeePlanInstallments. |
| 1582 | `POST /v1/finance/fee-plans/{feePlanId}/installments` | `FeePlanService/CreateFeePlanInstallment` | `finance.fee_plan_installment.create` |  | Create FeePlanInstallment. |
| 1583 | `GET /v1/finance/fee-plans/{feePlanId}/installments/{feePlanInstallmentId}` | `FeePlanService/GetFeePlanInstallment` | `finance.fee_plan_installment.read` |  | Get FeePlanInstallment. |
| 1584 | `PATCH /v1/finance/fee-plans/{feePlanId}/installments/{feePlanInstallmentId}` | `FeePlanService/UpdateFeePlanInstallment` | `finance.fee_plan_installment.update` |  | Update FeePlanInstallment. |
| 1585 | `DELETE /v1/finance/fee-plans/{feePlanId}/installments/{feePlanInstallmentId}` | `FeePlanService/DeleteFeePlanInstallment` | `finance.fee_plan_installment.delete` |  | Delete FeePlanInstallment. |
| 1586 | `GET /v1/finance/fee-plan-installment-components` | `FeePlanInstallmentComponentService/ListFeePlanInstallmentComponents` | `finance.fee_plan_installment_component.read` |  | List FeePlanInstallmentComponents. |
| 1587 | `POST /v1/finance/fee-plan-installment-components` | `FeePlanInstallmentComponentService/CreateFeePlanInstallmentComponent` | `finance.fee_plan_installment_component.create` |  | Create FeePlanInstallmentComponent. |
| 1588 | `GET /v1/finance/fee-plan-installment-components/{feePlanInstallmentComponentId}` | `FeePlanInstallmentComponentService/GetFeePlanInstallmentComponent` | `finance.fee_plan_installment_component.read` |  | Get FeePlanInstallmentComponent. |
| 1589 | `PATCH /v1/finance/fee-plan-installment-components/{feePlanInstallmentComponentId}` | `FeePlanInstallmentComponentService/UpdateFeePlanInstallmentComponent` | `finance.fee_plan_installment_component.update` |  | Update FeePlanInstallmentComponent. |
| 1590 | `DELETE /v1/finance/fee-plan-installment-components/{feePlanInstallmentComponentId}` | `FeePlanInstallmentComponentService/DeleteFeePlanInstallmentComponent` | `finance.fee_plan_installment_component.delete` |  | Delete FeePlanInstallmentComponent. |
| 1591 | `GET /v1/finance/student-fee-dues` | `StudentFeeDueService/ListStudentFeeDues` | `finance.student_fee_due.read` |  | List StudentFeeDues. |
| 1592 | `GET /v1/finance/student-fee-dues/{studentFeeDueId}` | `StudentFeeDueService/GetStudentFeeDue` | `finance.student_fee_due.read` |  | Get StudentFeeDue. |
| 1593 | `PATCH /v1/finance/student-fee-dues/{studentFeeDueId}` | `StudentFeeDueService/UpdateStudentFeeDue` | `finance.student_fee_due.update` |  | Update StudentFeeDue. |
| 1594 | `GET /v1/finance/invoices` | `InvoiceService/ListInvoices` | `finance.invoice.read` |  | List Invoices. |
| 1595 | `POST /v1/finance/invoices` | `InvoiceService/CreateInvoice` | `finance.invoice.create` |  | Create Invoice. |
| 1596 | `GET /v1/finance/invoices/{invoiceId}` | `InvoiceService/GetInvoice` | `finance.invoice.read` |  | Get Invoice. |
| 1597 | `PATCH /v1/finance/invoices/{invoiceId}` | `InvoiceService/UpdateInvoice` | `finance.invoice.update` |  | Update Invoice. |
| 1598 | `GET /v1/finance/invoices/{invoiceId}/lines` | `InvoiceService/ListInvoiceLines` | `finance.invoice_line.read` |  | List InvoiceLines. |
| 1599 | `GET /v1/finance/invoices/{invoiceId}/lines/{invoiceLineId}` | `InvoiceService/GetInvoiceLine` | `finance.invoice_line.read` |  | Get InvoiceLine. |
| 1600 | `GET /v1/finance/coupons` | `CouponService/ListCoupons` | `finance.coupon.read` |  | List Coupons. |
| 1601 | `POST /v1/finance/coupons` | `CouponService/CreateCoupon` | `finance.coupon.create` |  | Create Coupon. |
| 1602 | `GET /v1/finance/coupons/{couponId}` | `CouponService/GetCoupon` | `finance.coupon.read` |  | Get Coupon. |
| 1603 | `PATCH /v1/finance/coupons/{couponId}` | `CouponService/UpdateCoupon` | `finance.coupon.update` |  | Update Coupon. |
| 1604 | `DELETE /v1/finance/coupons/{couponId}` | `CouponService/DeleteCoupon` | `finance.coupon.delete` |  | Delete Coupon. |
| 1605 | `GET /v1/finance/coupons/{couponId}/scopes` | `CouponService/ListCouponScopes` | `finance.coupon_scope.read` |  | List CouponScopes. |
| 1606 | `POST /v1/finance/coupons/{couponId}/scopes` | `CouponService/CreateCouponScope` | `finance.coupon_scope.create` |  | Create CouponScope. |
| 1607 | `GET /v1/finance/coupons/{couponId}/scopes/{couponScopeId}` | `CouponService/GetCouponScope` | `finance.coupon_scope.read` |  | Get CouponScope. |
| 1608 | `PATCH /v1/finance/coupons/{couponId}/scopes/{couponScopeId}` | `CouponService/UpdateCouponScope` | `finance.coupon_scope.update` |  | Update CouponScope. |
| 1609 | `DELETE /v1/finance/coupons/{couponId}/scopes/{couponScopeId}` | `CouponService/DeleteCouponScope` | `finance.coupon_scope.delete` |  | Delete CouponScope. |
| 1610 | `GET /v1/finance/payments` | `PaymentService/ListPayments` | `finance.payment.read` |  | List Payments. |
| 1611 | `GET /v1/finance/payments/{paymentId}` | `PaymentService/GetPayment` | `finance.payment.read` |  | Get Payment. |
| 1612 | `POST /v1/finance/student-fee-dues:generate` | `StudentFeeDueService/GenerateFeeDues` | `finance.student_fee_due.generate_fee_dues` |  | Create dues for students from a fee plan. |
| 1613 | `POST /v1/finance/invoices:fromDues` | `InvoiceService/CreateInvoiceFromDues` | `finance.invoice.create_invoice_from_dues` |  | Raise an invoice for dues. |
| 1614 | `POST /v1/finance/invoices/{invoiceId}:post` | `InvoiceService/PostInvoice` | `finance.invoice.post_invoice` |  | Post (number assigned). |
| 1615 | `POST /v1/finance/invoices/{invoiceId}:cancel` | `InvoiceService/CancelInvoice` | `finance.invoice.cancel_invoice` |  | Cancel. |
| 1616 | `POST /v1/finance/invoices:sendReminders` | `InvoiceService/SendFeeReminders` | `finance.invoice.send_fee_reminders` |  | Remind guardians of unpaid fees. |
| 1617 | `GET /v1/finance/students/{studentId}/fee-statement` | `InvoiceService/GetFeeStatement` | `finance.invoice.get_fee_statement` |  | Student fee statement. |
| 1618 | `POST /v1/finance/checkout:quote` | `PaymentService/QuoteCheckout` | `finance.payment.quote_checkout` |  | Price with coupon, tax and currency. |
| 1619 | `POST /v1/finance/checkout:start` | `PaymentService/StartCheckout` | `finance.payment.start_checkout` |  | Create a payment and gateway order. |
| 1620 | `POST /v1/finance/payment-gateways/{gateway}:webhook` | `PaymentService/ReceiveGatewayWebhook` | `finance.gateway_event.receive_webhook` | Public | Gateway callback; marks payments received / failed and fulfils enrolments. |
| 1621 | `POST /v1/finance/payments/{paymentId}:refund` | `PaymentService/RefundPayment` | `finance.payment.refund_payment` |  | Refund through the gateway. |
| 1622 | `GET /v1/finance/acquisition-sources` | `AcquisitionSourceService/ListAcquisitionSources` | `finance.acquisition_source.read` |  | List AcquisitionSources. |
| 1623 | `POST /v1/finance/acquisition-sources` | `AcquisitionSourceService/CreateAcquisitionSource` | `finance.acquisition_source.create` |  | Create AcquisitionSource. |
| 1624 | `GET /v1/finance/acquisition-sources/{acquisitionSourceId}` | `AcquisitionSourceService/GetAcquisitionSource` | `finance.acquisition_source.read` |  | Get AcquisitionSource. |
| 1625 | `PATCH /v1/finance/acquisition-sources/{acquisitionSourceId}` | `AcquisitionSourceService/UpdateAcquisitionSource` | `finance.acquisition_source.update` |  | Update AcquisitionSource. |
| 1626 | `DELETE /v1/finance/acquisition-sources/{acquisitionSourceId}` | `AcquisitionSourceService/DeleteAcquisitionSource` | `finance.acquisition_source.delete` |  | Delete AcquisitionSource. |
| 1627 | `GET /v1/finance/fee-structures` | `FeeStructureService/ListFeeStructures` | `finance.fee_structure.read` |  | List FeeStructures. |
| 1628 | `POST /v1/finance/fee-structures` | `FeeStructureService/CreateFeeStructure` | `finance.fee_structure.create` |  | Create FeeStructure. |
| 1629 | `GET /v1/finance/fee-structures/{feeStructureId}` | `FeeStructureService/GetFeeStructure` | `finance.fee_structure.read` |  | Get FeeStructure. |
| 1630 | `PATCH /v1/finance/fee-structures/{feeStructureId}` | `FeeStructureService/UpdateFeeStructure` | `finance.fee_structure.update` |  | Update FeeStructure. |
| 1631 | `DELETE /v1/finance/fee-structures/{feeStructureId}` | `FeeStructureService/DeleteFeeStructure` | `finance.fee_structure.delete` |  | Delete FeeStructure. |
| 1632 | `POST /v1/finance/fee-structures/{feeStructureId}:publish` | `FeeStructureService/PublishFeeStructure` | `finance.fee_structure.publish` |  | Publish a fee structure and generate dues. |
| 1633 | `GET /v1/finance/fee-concessions` | `FeeConcessionService/ListFeeConcessions` | `finance.fee_concession.read` |  | List FeeConcessions. |
| 1634 | `POST /v1/finance/fee-concessions` | `FeeConcessionService/CreateFeeConcession` | `finance.fee_concession.create` |  | Create FeeConcession. |
| 1635 | `GET /v1/finance/fee-concessions/{feeConcessionId}` | `FeeConcessionService/GetFeeConcession` | `finance.fee_concession.read` |  | Get FeeConcession. |
| 1636 | `PATCH /v1/finance/fee-concessions/{feeConcessionId}` | `FeeConcessionService/UpdateFeeConcession` | `finance.fee_concession.update` |  | Update FeeConcession. |
| 1637 | `DELETE /v1/finance/fee-concessions/{feeConcessionId}` | `FeeConcessionService/DeleteFeeConcession` | `finance.fee_concession.delete` |  | Delete FeeConcession. |
| 1638 | `POST /v1/finance/payments:record` | `PaymentService/RecordPayment` | `finance.payment.record` |  | Record an offline/counter payment. |
| 1639 | `GET /v1/finance/refunds` | `RefundService/ListRefunds` | `finance.refund.read` |  | List Refunds. |
| 1640 | `GET /v1/finance/refunds/{refundId}` | `RefundService/GetRefund` | `finance.refund.read` |  | Get Refund. |
| 1641 | `GET /v1/finance/payment-gateway-accounts` | `PaymentGatewayAccountService/ListPaymentGatewayAccounts` | `finance.payment_gateway_account.read` |  | List PaymentGatewayAccounts. |
| 1642 | `POST /v1/finance/payment-gateway-accounts` | `PaymentGatewayAccountService/CreatePaymentGatewayAccount` | `finance.payment_gateway_account.create` |  | Create PaymentGatewayAccount. |
| 1643 | `GET /v1/finance/payment-gateway-accounts/{paymentGatewayAccountId}` | `PaymentGatewayAccountService/GetPaymentGatewayAccount` | `finance.payment_gateway_account.read` |  | Get PaymentGatewayAccount. |
| 1644 | `PATCH /v1/finance/payment-gateway-accounts/{paymentGatewayAccountId}` | `PaymentGatewayAccountService/UpdatePaymentGatewayAccount` | `finance.payment_gateway_account.update` |  | Update PaymentGatewayAccount. |
| 1645 | `DELETE /v1/finance/payment-gateway-accounts/{paymentGatewayAccountId}` | `PaymentGatewayAccountService/DeletePaymentGatewayAccount` | `finance.payment_gateway_account.delete` |  | Delete PaymentGatewayAccount. |
| 1646 | `POST /v1/finance/fee-structures/{feeStructureId}:archive` | `FeeStructureService/ArchiveFeeStructure` | `finance.fee_structure.archive` |  | Archive a fee structure. |
| 1647 | `GET /v1/finance/operations` | `OperationService/ListOperations` | `finance.operation.read` |  | Long-running operations of this module started by the caller. |
| 1648 | `GET /v1/finance/operations/{operationId}` | `OperationService/GetOperation` | `finance.operation.read` |  | Status of a long-running operation started in this module. |
| 1649 | `POST /v1/finance/operations/{operationId}:cancel` | `OperationService/CancelOperation` | `finance.operation.cancel_operation` |  | Request cancellation of a running operation. |

## Library (`library`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 1650 | `GET /v1/library/media-types` | `MediaTypeService/ListMediaTypes` | `library.media_type.read` |  | List MediaTypes. |
| 1651 | `POST /v1/library/media-types` | `MediaTypeService/CreateMediaType` | `library.media_type.create` |  | Create MediaType. |
| 1652 | `GET /v1/library/media-types/{mediaTypeId}` | `MediaTypeService/GetMediaType` | `library.media_type.read` |  | Get MediaType. |
| 1653 | `PATCH /v1/library/media-types/{mediaTypeId}` | `MediaTypeService/UpdateMediaType` | `library.media_type.update` |  | Update MediaType. |
| 1654 | `DELETE /v1/library/media-types/{mediaTypeId}` | `MediaTypeService/DeleteMediaType` | `library.media_type.delete` |  | Delete MediaType. |
| 1655 | `GET /v1/library/media-categories` | `MediaCategoryService/ListMediaCategories` | `library.media_category.read` |  | List MediaCategories. |
| 1656 | `POST /v1/library/media-categories` | `MediaCategoryService/CreateMediaCategory` | `library.media_category.create` |  | Create MediaCategory. |
| 1657 | `GET /v1/library/media-categories/{mediaCategoryId}` | `MediaCategoryService/GetMediaCategory` | `library.media_category.read` |  | Get MediaCategory. |
| 1658 | `PATCH /v1/library/media-categories/{mediaCategoryId}` | `MediaCategoryService/UpdateMediaCategory` | `library.media_category.update` |  | Update MediaCategory. |
| 1659 | `DELETE /v1/library/media-categories/{mediaCategoryId}` | `MediaCategoryService/DeleteMediaCategory` | `library.media_category.delete` |  | Delete MediaCategory. |
| 1660 | `GET /v1/library/authors` | `AuthorService/ListAuthors` | `library.author.read` |  | List Authors. |
| 1661 | `POST /v1/library/authors` | `AuthorService/CreateAuthor` | `library.author.create` |  | Create Author. |
| 1662 | `GET /v1/library/authors/{authorId}` | `AuthorService/GetAuthor` | `library.author.read` |  | Get Author. |
| 1663 | `PATCH /v1/library/authors/{authorId}` | `AuthorService/UpdateAuthor` | `library.author.update` |  | Update Author. |
| 1664 | `DELETE /v1/library/authors/{authorId}` | `AuthorService/DeleteAuthor` | `library.author.delete` |  | Delete Author. |
| 1665 | `GET /v1/library/publishers` | `PublisherService/ListPublishers` | `library.publisher.read` |  | List Publishers. |
| 1666 | `POST /v1/library/publishers` | `PublisherService/CreatePublisher` | `library.publisher.create` |  | Create Publisher. |
| 1667 | `GET /v1/library/publishers/{publisherId}` | `PublisherService/GetPublisher` | `library.publisher.read` |  | Get Publisher. |
| 1668 | `PATCH /v1/library/publishers/{publisherId}` | `PublisherService/UpdatePublisher` | `library.publisher.update` |  | Update Publisher. |
| 1669 | `DELETE /v1/library/publishers/{publisherId}` | `PublisherService/DeletePublisher` | `library.publisher.delete` |  | Delete Publisher. |
| 1670 | `GET /v1/library/media` | `MediaService/ListMedia` | `library.media.read` |  | List Media. |
| 1671 | `POST /v1/library/media` | `MediaService/CreateMedia` | `library.media.create` |  | Create Media. |
| 1672 | `GET /v1/library/media/{mediaId}` | `MediaService/GetMedia` | `library.media.read` |  | Get Media. |
| 1673 | `PATCH /v1/library/media/{mediaId}` | `MediaService/UpdateMedia` | `library.media.update` |  | Update Media. |
| 1674 | `DELETE /v1/library/media/{mediaId}` | `MediaService/DeleteMedia` | `library.media.delete` |  | Delete Media. |
| 1675 | `GET /v1/library/media/{mediaId}/authors` | `MediaService/ListMediaAuthors` | `library.media_author.read` |  | List MediaAuthors. |
| 1676 | `POST /v1/library/media/{mediaId}/authors` | `MediaService/CreateMediaAuthor` | `library.media_author.create` |  | Create MediaAuthor. |
| 1677 | `GET /v1/library/media/{mediaId}/authors/{mediaAuthorId}` | `MediaService/GetMediaAuthor` | `library.media_author.read` |  | Get MediaAuthor. |
| 1678 | `PATCH /v1/library/media/{mediaId}/authors/{mediaAuthorId}` | `MediaService/UpdateMediaAuthor` | `library.media_author.update` |  | Update MediaAuthor. |
| 1679 | `DELETE /v1/library/media/{mediaId}/authors/{mediaAuthorId}` | `MediaService/DeleteMediaAuthor` | `library.media_author.delete` |  | Delete MediaAuthor. |
| 1680 | `GET /v1/library/media/{mediaId}/publishers` | `MediaService/ListMediaPublishers` | `library.media_publisher.read` |  | List MediaPublishers. |
| 1681 | `POST /v1/library/media/{mediaId}/publishers` | `MediaService/CreateMediaPublisher` | `library.media_publisher.create` |  | Create MediaPublisher. |
| 1682 | `GET /v1/library/media/{mediaId}/publishers/{mediaPublisherId}` | `MediaService/GetMediaPublisher` | `library.media_publisher.read` |  | Get MediaPublisher. |
| 1683 | `PATCH /v1/library/media/{mediaId}/publishers/{mediaPublisherId}` | `MediaService/UpdateMediaPublisher` | `library.media_publisher.update` |  | Update MediaPublisher. |
| 1684 | `DELETE /v1/library/media/{mediaId}/publishers/{mediaPublisherId}` | `MediaService/DeleteMediaPublisher` | `library.media_publisher.delete` |  | Delete MediaPublisher. |
| 1685 | `GET /v1/library/media/{mediaId}/courses` | `MediaService/ListMediaCourses` | `library.media_course.read` |  | List MediaCourses. |
| 1686 | `POST /v1/library/media/{mediaId}/courses` | `MediaService/CreateMediaCourse` | `library.media_course.create` |  | Create MediaCourse. |
| 1687 | `GET /v1/library/media/{mediaId}/courses/{mediaCourseId}` | `MediaService/GetMediaCourse` | `library.media_course.read` |  | Get MediaCourse. |
| 1688 | `PATCH /v1/library/media/{mediaId}/courses/{mediaCourseId}` | `MediaService/UpdateMediaCourse` | `library.media_course.update` |  | Update MediaCourse. |
| 1689 | `DELETE /v1/library/media/{mediaId}/courses/{mediaCourseId}` | `MediaService/DeleteMediaCourse` | `library.media_course.delete` |  | Delete MediaCourse. |
| 1690 | `GET /v1/library/media/{mediaId}/subjects` | `MediaService/ListMediaSubjects` | `library.media_subject.read` |  | List MediaSubjects. |
| 1691 | `POST /v1/library/media/{mediaId}/subjects` | `MediaService/CreateMediaSubject` | `library.media_subject.create` |  | Create MediaSubject. |
| 1692 | `GET /v1/library/media/{mediaId}/subjects/{mediaSubjectId}` | `MediaService/GetMediaSubject` | `library.media_subject.read` |  | Get MediaSubject. |
| 1693 | `PATCH /v1/library/media/{mediaId}/subjects/{mediaSubjectId}` | `MediaService/UpdateMediaSubject` | `library.media_subject.update` |  | Update MediaSubject. |
| 1694 | `DELETE /v1/library/media/{mediaId}/subjects/{mediaSubjectId}` | `MediaService/DeleteMediaSubject` | `library.media_subject.delete` |  | Delete MediaSubject. |
| 1695 | `GET /v1/library/media/{mediaId}/units` | `MediaService/ListMediaUnits` | `library.media_unit.read` |  | List MediaUnits. |
| 1696 | `POST /v1/library/media/{mediaId}/units` | `MediaService/CreateMediaUnit` | `library.media_unit.create` |  | Create MediaUnit. |
| 1697 | `GET /v1/library/media/{mediaId}/units/{mediaUnitId}` | `MediaService/GetMediaUnit` | `library.media_unit.read` |  | Get MediaUnit. |
| 1698 | `PATCH /v1/library/media/{mediaId}/units/{mediaUnitId}` | `MediaService/UpdateMediaUnit` | `library.media_unit.update` |  | Update MediaUnit. |
| 1699 | `DELETE /v1/library/media/{mediaId}/units/{mediaUnitId}` | `MediaService/DeleteMediaUnit` | `library.media_unit.delete` |  | Delete MediaUnit. |
| 1700 | `GET /v1/library/library-card-types` | `LibraryCardTypeService/ListLibraryCardTypes` | `library.library_card_type.read` |  | List LibraryCardTypes. |
| 1701 | `POST /v1/library/library-card-types` | `LibraryCardTypeService/CreateLibraryCardType` | `library.library_card_type.create` |  | Create LibraryCardType. |
| 1702 | `GET /v1/library/library-card-types/{libraryCardTypeId}` | `LibraryCardTypeService/GetLibraryCardType` | `library.library_card_type.read` |  | Get LibraryCardType. |
| 1703 | `PATCH /v1/library/library-card-types/{libraryCardTypeId}` | `LibraryCardTypeService/UpdateLibraryCardType` | `library.library_card_type.update` |  | Update LibraryCardType. |
| 1704 | `DELETE /v1/library/library-card-types/{libraryCardTypeId}` | `LibraryCardTypeService/DeleteLibraryCardType` | `library.library_card_type.delete` |  | Delete LibraryCardType. |
| 1705 | `GET /v1/library/library-cards` | `LibraryCardService/ListLibraryCards` | `library.library_card.read` |  | List LibraryCards. |
| 1706 | `POST /v1/library/library-cards` | `LibraryCardService/CreateLibraryCard` | `library.library_card.create` |  | Create LibraryCard. |
| 1707 | `GET /v1/library/library-cards/{libraryCardId}` | `LibraryCardService/GetLibraryCard` | `library.library_card.read` |  | Get LibraryCard. |
| 1708 | `PATCH /v1/library/library-cards/{libraryCardId}` | `LibraryCardService/UpdateLibraryCard` | `library.library_card.update` |  | Update LibraryCard. |
| 1709 | `DELETE /v1/library/library-cards/{libraryCardId}` | `LibraryCardService/DeleteLibraryCard` | `library.library_card.delete` |  | Delete LibraryCard. |
| 1710 | `GET /v1/library/loans` | `LoanService/ListLoans` | `library.loan.read` |  | List Loans. |
| 1711 | `POST /v1/library/loans` | `LoanService/CreateLoan` | `library.loan.create` |  | Create Loan. |
| 1712 | `GET /v1/library/loans/{loanId}` | `LoanService/GetLoan` | `library.loan.read` |  | Get Loan. |
| 1713 | `PATCH /v1/library/loans/{loanId}` | `LoanService/UpdateLoan` | `library.loan.update` |  | Update Loan. |
| 1714 | `DELETE /v1/library/loans/{loanId}` | `LoanService/DeleteLoan` | `library.loan.delete` |  | Delete Loan. |
| 1715 | `GET /v1/library/reservations` | `ReservationService/ListReservations` | `library.reservation.read` |  | List Reservations. |
| 1716 | `POST /v1/library/reservations` | `ReservationService/CreateReservation` | `library.reservation.create` |  | Create Reservation. |
| 1717 | `GET /v1/library/reservations/{reservationId}` | `ReservationService/GetReservation` | `library.reservation.read` |  | Get Reservation. |
| 1718 | `PATCH /v1/library/reservations/{reservationId}` | `ReservationService/UpdateReservation` | `library.reservation.update` |  | Update Reservation. |
| 1719 | `DELETE /v1/library/reservations/{reservationId}` | `ReservationService/DeleteReservation` | `library.reservation.delete` |  | Delete Reservation. |
| 1720 | `GET /v1/library/purchase-requests` | `PurchaseRequestService/ListPurchaseRequests` | `library.purchase_request.read` |  | List PurchaseRequests. |
| 1721 | `POST /v1/library/purchase-requests` | `PurchaseRequestService/CreatePurchaseRequest` | `library.purchase_request.create` |  | Create PurchaseRequest. |
| 1722 | `GET /v1/library/purchase-requests/{purchaseRequestId}` | `PurchaseRequestService/GetPurchaseRequest` | `library.purchase_request.read` |  | Get PurchaseRequest. |
| 1723 | `PATCH /v1/library/purchase-requests/{purchaseRequestId}` | `PurchaseRequestService/UpdatePurchaseRequest` | `library.purchase_request.update` |  | Update PurchaseRequest. |
| 1724 | `DELETE /v1/library/purchase-requests/{purchaseRequestId}` | `PurchaseRequestService/DeletePurchaseRequest` | `library.purchase_request.delete` |  | Delete PurchaseRequest. |
| 1725 | `GET /v1/library/purchase-requests/{purchaseRequestId}/courses` | `PurchaseRequestService/ListPurchaseRequestCourses` | `library.purchase_request_course.read` |  | List PurchaseRequestCourses. |
| 1726 | `POST /v1/library/purchase-requests/{purchaseRequestId}/courses` | `PurchaseRequestService/CreatePurchaseRequestCourse` | `library.purchase_request_course.create` |  | Create PurchaseRequestCourse. |
| 1727 | `GET /v1/library/purchase-requests/{purchaseRequestId}/courses/{purchaseRequestCourseId}` | `PurchaseRequestService/GetPurchaseRequestCourse` | `library.purchase_request_course.read` |  | Get PurchaseRequestCourse. |
| 1728 | `PATCH /v1/library/purchase-requests/{purchaseRequestId}/courses/{purchaseRequestCourseId}` | `PurchaseRequestService/UpdatePurchaseRequestCourse` | `library.purchase_request_course.update` |  | Update PurchaseRequestCourse. |
| 1729 | `DELETE /v1/library/purchase-requests/{purchaseRequestId}/courses/{purchaseRequestCourseId}` | `PurchaseRequestService/DeletePurchaseRequestCourse` | `library.purchase_request_course.delete` |  | Delete PurchaseRequestCourse. |
| 1730 | `GET /v1/library/purchase-requests/{purchaseRequestId}/subjects` | `PurchaseRequestService/ListPurchaseRequestSubjects` | `library.purchase_request_subject.read` |  | List PurchaseRequestSubjects. |
| 1731 | `POST /v1/library/purchase-requests/{purchaseRequestId}/subjects` | `PurchaseRequestService/CreatePurchaseRequestSubject` | `library.purchase_request_subject.create` |  | Create PurchaseRequestSubject. |
| 1732 | `GET /v1/library/purchase-requests/{purchaseRequestId}/subjects/{purchaseRequestSubjectId}` | `PurchaseRequestService/GetPurchaseRequestSubject` | `library.purchase_request_subject.read` |  | Get PurchaseRequestSubject. |
| 1733 | `PATCH /v1/library/purchase-requests/{purchaseRequestId}/subjects/{purchaseRequestSubjectId}` | `PurchaseRequestService/UpdatePurchaseRequestSubject` | `library.purchase_request_subject.update` |  | Update PurchaseRequestSubject. |
| 1734 | `DELETE /v1/library/purchase-requests/{purchaseRequestId}/subjects/{purchaseRequestSubjectId}` | `PurchaseRequestService/DeletePurchaseRequestSubject` | `library.purchase_request_subject.delete` |  | Delete PurchaseRequestSubject. |
| 1735 | `POST /v1/library/loans/{loanId}:requestReturn` | `LoanService/RequestLoanReturn` | `library.loan.request_loan_return` |  | Borrower drops the copy for return (status return_pending until checked). |
| 1736 | `POST /v1/library/loans/{loanId}:return` | `LoanService/ReturnLoan` | `library.loan.return_loan` |  | Librarian checks the copy in (status returned); fine is computed. |
| 1737 | `POST /v1/library/loans/{loanId}:markLost` | `LoanService/MarkLoanLost` | `library.loan.mark_loan_lost` |  | Copy lost. |
| 1738 | `POST /v1/library/loans/{loanId}:invoiceFine` | `LoanService/InvoiceLoanFine` | `library.loan.invoice_loan_fine` |  | Raise an invoice for the fine. |
| 1739 | `POST /v1/library/reservations/{reservationId}:accept` | `ReservationService/AcceptReservation` | `library.reservation.accept_reservation` |  | Accept. |
| 1740 | `POST /v1/library/reservations/{reservationId}:reject` | `ReservationService/RejectReservation` | `library.reservation.reject_reservation` |  | Reject. |
| 1741 | `POST /v1/library/reservations/{reservationId}:requestAgain` | `ReservationService/RequestReservationAgain` | `library.reservation.request_reservation_again` |  | Re-request. |
| 1742 | `POST /v1/library/purchase-requests/{purchaseRequestId}:submit` | `PurchaseRequestService/SubmitPurchaseRequest` | `library.purchase_request.submit_purchase_request` |  | Send to the librarian. |
| 1743 | `POST /v1/library/purchase-requests/{purchaseRequestId}:accept` | `PurchaseRequestService/AcceptPurchaseRequest` | `library.purchase_request.accept_purchase_request` |  | Accept. |
| 1744 | `POST /v1/library/purchase-requests/{purchaseRequestId}:reject` | `PurchaseRequestService/RejectPurchaseRequest` | `library.purchase_request.reject_purchase_request` |  | Reject. |

## Transport (`transport`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 1745 | `GET /v1/transport/routes` | `RouteService/ListRoutes` | `transport.route.read` |  | List Routes. |
| 1746 | `POST /v1/transport/routes` | `RouteService/CreateRoute` | `transport.route.create` |  | Create Route. |
| 1747 | `GET /v1/transport/routes/{routeId}` | `RouteService/GetRoute` | `transport.route.read` |  | Get Route. |
| 1748 | `PATCH /v1/transport/routes/{routeId}` | `RouteService/UpdateRoute` | `transport.route.update` |  | Update Route. |
| 1749 | `DELETE /v1/transport/routes/{routeId}` | `RouteService/DeleteRoute` | `transport.route.delete` |  | Delete Route. |
| 1750 | `GET /v1/transport/routes/{routeId}/stops` | `RouteService/ListRouteStops` | `transport.route_stop.read` |  | List RouteStops. |
| 1751 | `POST /v1/transport/routes/{routeId}/stops` | `RouteService/CreateRouteStop` | `transport.route_stop.create` |  | Create RouteStop. |
| 1752 | `GET /v1/transport/routes/{routeId}/stops/{routeStopId}` | `RouteService/GetRouteStop` | `transport.route_stop.read` |  | Get RouteStop. |
| 1753 | `PATCH /v1/transport/routes/{routeId}/stops/{routeStopId}` | `RouteService/UpdateRouteStop` | `transport.route_stop.update` |  | Update RouteStop. |
| 1754 | `DELETE /v1/transport/routes/{routeId}/stops/{routeStopId}` | `RouteService/DeleteRouteStop` | `transport.route_stop.delete` |  | Delete RouteStop. |
| 1755 | `GET /v1/transport/student-transports` | `StudentTransportService/ListStudentTransports` | `transport.student_transport.read` |  | List StudentTransports. |
| 1756 | `POST /v1/transport/student-transports` | `StudentTransportService/CreateStudentTransport` | `transport.student_transport.create` |  | Create StudentTransport. |
| 1757 | `GET /v1/transport/student-transports/{studentTransportId}` | `StudentTransportService/GetStudentTransport` | `transport.student_transport.read` |  | Get StudentTransport. |
| 1758 | `PATCH /v1/transport/student-transports/{studentTransportId}` | `StudentTransportService/UpdateStudentTransport` | `transport.student_transport.update` |  | Update StudentTransport. |
| 1759 | `DELETE /v1/transport/student-transports/{studentTransportId}` | `StudentTransportService/DeleteStudentTransport` | `transport.student_transport.delete` |  | Delete StudentTransport. |

## Communication & Community (`comms`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 1760 | `GET /v1/comms/message-templates` | `MessageTemplateService/ListMessageTemplates` | `comms.message_template.read` |  | List MessageTemplates. |
| 1761 | `POST /v1/comms/message-templates` | `MessageTemplateService/CreateMessageTemplate` | `comms.message_template.create` |  | Create MessageTemplate. |
| 1762 | `GET /v1/comms/message-templates/{messageTemplateId}` | `MessageTemplateService/GetMessageTemplate` | `comms.message_template.read` |  | Get MessageTemplate. |
| 1763 | `PATCH /v1/comms/message-templates/{messageTemplateId}` | `MessageTemplateService/UpdateMessageTemplate` | `comms.message_template.update` |  | Update MessageTemplate. |
| 1764 | `DELETE /v1/comms/message-templates/{messageTemplateId}` | `MessageTemplateService/DeleteMessageTemplate` | `comms.message_template.delete` |  | Delete MessageTemplate. |
| 1765 | `GET /v1/comms/outbound-messages` | `OutboundMessageService/ListOutboundMessages` | `comms.outbound_message.read` |  | List OutboundMessages. |
| 1766 | `GET /v1/comms/outbound-messages/{outboundMessageId}` | `OutboundMessageService/GetOutboundMessage` | `comms.outbound_message.read` |  | Get OutboundMessage. |
| 1767 | `GET /v1/comms/mailbox-messages` | `MailboxMessageService/ListMailboxMessages` | `comms.mailbox_message.read` |  | List MailboxMessages. |
| 1768 | `GET /v1/comms/mailbox-messages/{mailboxMessageId}` | `MailboxMessageService/GetMailboxMessage` | `comms.mailbox_message.read` |  | Get MailboxMessage. |
| 1769 | `DELETE /v1/comms/mailbox-messages/{mailboxMessageId}` | `MailboxMessageService/DeleteMailboxMessage` | `comms.mailbox_message.delete` |  | Delete MailboxMessage. |
| 1770 | `GET /v1/comms/notifications` | `InAppNotificationService/ListInAppNotifications` | `comms.in_app_notification.read` |  | List InAppNotifications. |
| 1771 | `GET /v1/comms/notifications/{inAppNotificationId}` | `InAppNotificationService/GetInAppNotification` | `comms.in_app_notification.read` |  | Get InAppNotification. |
| 1772 | `GET /v1/comms/notice-backgrounds` | `NoticeBackgroundService/ListNoticeBackgrounds` | `comms.notice_background.read` |  | List NoticeBackgrounds. |
| 1773 | `POST /v1/comms/notice-backgrounds` | `NoticeBackgroundService/CreateNoticeBackground` | `comms.notice_background.create` |  | Create NoticeBackground. |
| 1774 | `GET /v1/comms/notice-backgrounds/{noticeBackgroundId}` | `NoticeBackgroundService/GetNoticeBackground` | `comms.notice_background.read` |  | Get NoticeBackground. |
| 1775 | `PATCH /v1/comms/notice-backgrounds/{noticeBackgroundId}` | `NoticeBackgroundService/UpdateNoticeBackground` | `comms.notice_background.update` |  | Update NoticeBackground. |
| 1776 | `DELETE /v1/comms/notice-backgrounds/{noticeBackgroundId}` | `NoticeBackgroundService/DeleteNoticeBackground` | `comms.notice_background.delete` |  | Delete NoticeBackground. |
| 1777 | `GET /v1/comms/notices` | `NoticeService/ListNotices` | `comms.notice.read` |  | List Notices. |
| 1778 | `POST /v1/comms/notices` | `NoticeService/CreateNotice` | `comms.notice.create` |  | Create Notice. |
| 1779 | `GET /v1/comms/notices/{noticeId}` | `NoticeService/GetNotice` | `comms.notice.read` |  | Get Notice. |
| 1780 | `PATCH /v1/comms/notices/{noticeId}` | `NoticeService/UpdateNotice` | `comms.notice.update` |  | Update Notice. |
| 1781 | `DELETE /v1/comms/notices/{noticeId}` | `NoticeService/DeleteNotice` | `comms.notice.delete` |  | Delete Notice. |
| 1782 | `GET /v1/comms/events` | `EventService/ListEvents` | `comms.event.read` |  | List Events. |
| 1783 | `POST /v1/comms/events` | `EventService/CreateEvent` | `comms.event.create` |  | Create Event. |
| 1784 | `GET /v1/comms/events/{eventId}` | `EventService/GetEvent` | `comms.event.read` |  | Get Event. |
| 1785 | `PATCH /v1/comms/events/{eventId}` | `EventService/UpdateEvent` | `comms.event.update` |  | Update Event. |
| 1786 | `DELETE /v1/comms/events/{eventId}` | `EventService/DeleteEvent` | `comms.event.delete` |  | Delete Event. |
| 1787 | `GET /v1/comms/events/{eventId}/gallery-items` | `EventService/ListEventGalleryItems` | `comms.event_gallery_item.read` |  | List EventGalleryItems. |
| 1788 | `POST /v1/comms/events/{eventId}/gallery-items` | `EventService/CreateEventGalleryItem` | `comms.event_gallery_item.create` |  | Create EventGalleryItem. |
| 1789 | `GET /v1/comms/events/{eventId}/gallery-items/{eventGalleryItemId}` | `EventService/GetEventGalleryItem` | `comms.event_gallery_item.read` |  | Get EventGalleryItem. |
| 1790 | `PATCH /v1/comms/events/{eventId}/gallery-items/{eventGalleryItemId}` | `EventService/UpdateEventGalleryItem` | `comms.event_gallery_item.update` |  | Update EventGalleryItem. |
| 1791 | `DELETE /v1/comms/events/{eventId}/gallery-items/{eventGalleryItemId}` | `EventService/DeleteEventGalleryItem` | `comms.event_gallery_item.delete` |  | Delete EventGalleryItem. |
| 1792 | `GET /v1/comms/magazines` | `MagazineService/ListMagazines` | `comms.magazine.read` |  | List Magazines. |
| 1793 | `POST /v1/comms/magazines` | `MagazineService/CreateMagazine` | `comms.magazine.create` |  | Create Magazine. |
| 1794 | `GET /v1/comms/magazines/{magazineId}` | `MagazineService/GetMagazine` | `comms.magazine.read` |  | Get Magazine. |
| 1795 | `PATCH /v1/comms/magazines/{magazineId}` | `MagazineService/UpdateMagazine` | `comms.magazine.update` |  | Update Magazine. |
| 1796 | `DELETE /v1/comms/magazines/{magazineId}` | `MagazineService/DeleteMagazine` | `comms.magazine.delete` |  | Delete Magazine. |
| 1797 | `GET /v1/comms/conversations` | `ConversationService/ListConversations` | `comms.conversation.read` |  | List Conversations. |
| 1798 | `POST /v1/comms/conversations` | `ConversationService/CreateConversation` | `comms.conversation.create` |  | Create Conversation. |
| 1799 | `GET /v1/comms/conversations/{conversationId}` | `ConversationService/GetConversation` | `comms.conversation.read` |  | Get Conversation. |
| 1800 | `PATCH /v1/comms/conversations/{conversationId}` | `ConversationService/UpdateConversation` | `comms.conversation.update` |  | Update Conversation. |
| 1801 | `DELETE /v1/comms/conversations/{conversationId}` | `ConversationService/DeleteConversation` | `comms.conversation.delete` |  | Delete Conversation. |
| 1802 | `GET /v1/comms/conversations/{conversationId}/participants` | `ConversationService/ListConversationParticipants` | `comms.conversation_participant.read` |  | List ConversationParticipants. |
| 1803 | `POST /v1/comms/conversations/{conversationId}/participants` | `ConversationService/CreateConversationParticipant` | `comms.conversation_participant.create` |  | Create ConversationParticipant. |
| 1804 | `GET /v1/comms/conversations/{conversationId}/participants/{conversationParticipantId}` | `ConversationService/GetConversationParticipant` | `comms.conversation_participant.read` |  | Get ConversationParticipant. |
| 1805 | `PATCH /v1/comms/conversations/{conversationId}/participants/{conversationParticipantId}` | `ConversationService/UpdateConversationParticipant` | `comms.conversation_participant.update` |  | Update ConversationParticipant. |
| 1806 | `DELETE /v1/comms/conversations/{conversationId}/participants/{conversationParticipantId}` | `ConversationService/DeleteConversationParticipant` | `comms.conversation_participant.delete` |  | Delete ConversationParticipant. |
| 1807 | `GET /v1/comms/conversations/{conversationId}/messages` | `ConversationService/ListConversationMessages` | `comms.conversation_message.read` |  | List ConversationMessages. |
| 1808 | `POST /v1/comms/conversations/{conversationId}/messages` | `ConversationService/CreateConversationMessage` | `comms.conversation_message.create` |  | Create ConversationMessage. |
| 1809 | `GET /v1/comms/conversations/{conversationId}/messages/{conversationMessageId}` | `ConversationService/GetConversationMessage` | `comms.conversation_message.read` |  | Get ConversationMessage. |
| 1810 | `PATCH /v1/comms/conversations/{conversationId}/messages/{conversationMessageId}` | `ConversationService/UpdateConversationMessage` | `comms.conversation_message.update` |  | Update ConversationMessage. |
| 1811 | `DELETE /v1/comms/conversations/{conversationId}/messages/{conversationMessageId}` | `ConversationService/DeleteConversationMessage` | `comms.conversation_message.delete` |  | Delete ConversationMessage. |
| 1812 | `GET /v1/comms/feedback-threads` | `FeedbackThreadService/ListFeedbackThreads` | `comms.feedback_thread.read` |  | List FeedbackThreads. |
| 1813 | `POST /v1/comms/feedback-threads` | `FeedbackThreadService/CreateFeedbackThread` | `comms.feedback_thread.create` |  | Create FeedbackThread. |
| 1814 | `GET /v1/comms/feedback-threads/{feedbackThreadId}` | `FeedbackThreadService/GetFeedbackThread` | `comms.feedback_thread.read` |  | Get FeedbackThread. |
| 1815 | `PATCH /v1/comms/feedback-threads/{feedbackThreadId}` | `FeedbackThreadService/UpdateFeedbackThread` | `comms.feedback_thread.update` |  | Update FeedbackThread. |
| 1816 | `DELETE /v1/comms/feedback-threads/{feedbackThreadId}` | `FeedbackThreadService/DeleteFeedbackThread` | `comms.feedback_thread.delete` |  | Delete FeedbackThread. |
| 1817 | `GET /v1/comms/feedback-threads/{feedbackThreadId}/messages` | `FeedbackThreadService/ListFeedbackMessages` | `comms.feedback_message.read` |  | List FeedbackMessages. |
| 1818 | `POST /v1/comms/feedback-threads/{feedbackThreadId}/messages` | `FeedbackThreadService/CreateFeedbackMessage` | `comms.feedback_message.create` |  | Create FeedbackMessage. |
| 1819 | `GET /v1/comms/feedback-threads/{feedbackThreadId}/messages/{feedbackMessageId}` | `FeedbackThreadService/GetFeedbackMessage` | `comms.feedback_message.read` |  | Get FeedbackMessage. |
| 1820 | `PATCH /v1/comms/feedback-threads/{feedbackThreadId}/messages/{feedbackMessageId}` | `FeedbackThreadService/UpdateFeedbackMessage` | `comms.feedback_message.update` |  | Update FeedbackMessage. |
| 1821 | `DELETE /v1/comms/feedback-threads/{feedbackThreadId}/messages/{feedbackMessageId}` | `FeedbackThreadService/DeleteFeedbackMessage` | `comms.feedback_message.delete` |  | Delete FeedbackMessage. |
| 1822 | `GET /v1/comms/audience-groups` | `AudienceGroupService/ListAudienceGroups` | `comms.audience_group.read` |  | List AudienceGroups. |
| 1823 | `POST /v1/comms/audience-groups` | `AudienceGroupService/CreateAudienceGroup` | `comms.audience_group.create` |  | Create AudienceGroup. |
| 1824 | `GET /v1/comms/audience-groups/{audienceGroupId}` | `AudienceGroupService/GetAudienceGroup` | `comms.audience_group.read` |  | Get AudienceGroup. |
| 1825 | `PATCH /v1/comms/audience-groups/{audienceGroupId}` | `AudienceGroupService/UpdateAudienceGroup` | `comms.audience_group.update` |  | Update AudienceGroup. |
| 1826 | `DELETE /v1/comms/audience-groups/{audienceGroupId}` | `AudienceGroupService/DeleteAudienceGroup` | `comms.audience_group.delete` |  | Delete AudienceGroup. |
| 1827 | `GET /v1/comms/audience-groups/{audienceGroupId}/members` | `AudienceGroupService/ListAudienceGroupMembers` | `comms.audience_group_member.read` |  | List AudienceGroupMembers. |
| 1828 | `POST /v1/comms/audience-groups/{audienceGroupId}/members` | `AudienceGroupService/CreateAudienceGroupMember` | `comms.audience_group_member.create` |  | Create AudienceGroupMember. |
| 1829 | `GET /v1/comms/audience-groups/{audienceGroupId}/members/{audienceGroupMemberId}` | `AudienceGroupService/GetAudienceGroupMember` | `comms.audience_group_member.read` |  | Get AudienceGroupMember. |
| 1830 | `PATCH /v1/comms/audience-groups/{audienceGroupId}/members/{audienceGroupMemberId}` | `AudienceGroupService/UpdateAudienceGroupMember` | `comms.audience_group_member.update` |  | Update AudienceGroupMember. |
| 1831 | `DELETE /v1/comms/audience-groups/{audienceGroupId}/members/{audienceGroupMemberId}` | `AudienceGroupService/DeleteAudienceGroupMember` | `comms.audience_group_member.delete` |  | Delete AudienceGroupMember. |
| 1832 | `GET /v1/comms/tasks` | `TaskService/ListTasks` | `comms.task.read` |  | List Tasks. |
| 1833 | `POST /v1/comms/tasks` | `TaskService/CreateTask` | `comms.task.create` |  | Create Task. |
| 1834 | `GET /v1/comms/tasks/{taskId}` | `TaskService/GetTask` | `comms.task.read` |  | Get Task. |
| 1835 | `PATCH /v1/comms/tasks/{taskId}` | `TaskService/UpdateTask` | `comms.task.update` |  | Update Task. |
| 1836 | `DELETE /v1/comms/tasks/{taskId}` | `TaskService/DeleteTask` | `comms.task.delete` |  | Delete Task. |
| 1837 | `GET /v1/comms/tasks/{taskId}/assignees` | `TaskService/ListTaskAssignees` | `comms.task_assignee.read` |  | List TaskAssignees. |
| 1838 | `POST /v1/comms/tasks/{taskId}/assignees` | `TaskService/CreateTaskAssignee` | `comms.task_assignee.create` |  | Create TaskAssignee. |
| 1839 | `GET /v1/comms/tasks/{taskId}/assignees/{taskAssigneeId}` | `TaskService/GetTaskAssignee` | `comms.task_assignee.read` |  | Get TaskAssignee. |
| 1840 | `PATCH /v1/comms/tasks/{taskId}/assignees/{taskAssigneeId}` | `TaskService/UpdateTaskAssignee` | `comms.task_assignee.update` |  | Update TaskAssignee. |
| 1841 | `DELETE /v1/comms/tasks/{taskId}/assignees/{taskAssigneeId}` | `TaskService/DeleteTaskAssignee` | `comms.task_assignee.delete` |  | Delete TaskAssignee. |
| 1842 | `GET /v1/comms/content-views` | `ContentViewService/ListContentViews` | `comms.content_view.read` |  | List ContentViews. |
| 1843 | `POST /v1/comms/content-views` | `ContentViewService/CreateContentView` | `comms.content_view.create` |  | Create ContentView. |
| 1844 | `GET /v1/comms/content-views/{contentViewId}` | `ContentViewService/GetContentView` | `comms.content_view.read` |  | Get ContentView. |
| 1845 | `PATCH /v1/comms/content-views/{contentViewId}` | `ContentViewService/UpdateContentView` | `comms.content_view.update` |  | Update ContentView. |
| 1846 | `DELETE /v1/comms/content-views/{contentViewId}` | `ContentViewService/DeleteContentView` | `comms.content_view.delete` |  | Delete ContentView. |
| 1847 | `GET /v1/comms/page-categories` | `PageCategoryService/ListPageCategories` | `comms.page_category.read` |  | List PageCategories. |
| 1848 | `POST /v1/comms/page-categories` | `PageCategoryService/CreatePageCategory` | `comms.page_category.create` |  | Create PageCategory. |
| 1849 | `GET /v1/comms/page-categories/{pageCategoryId}` | `PageCategoryService/GetPageCategory` | `comms.page_category.read` |  | Get PageCategory. |
| 1850 | `PATCH /v1/comms/page-categories/{pageCategoryId}` | `PageCategoryService/UpdatePageCategory` | `comms.page_category.update` |  | Update PageCategory. |
| 1851 | `DELETE /v1/comms/page-categories/{pageCategoryId}` | `PageCategoryService/DeletePageCategory` | `comms.page_category.delete` |  | Delete PageCategory. |
| 1852 | `GET /v1/comms/class-pages` | `ClassPageService/ListClassPages` | `comms.class_page.read` |  | List ClassPages. |
| 1853 | `POST /v1/comms/class-pages` | `ClassPageService/CreateClassPage` | `comms.class_page.create` |  | Create ClassPage. |
| 1854 | `GET /v1/comms/class-pages/{classPageId}` | `ClassPageService/GetClassPage` | `comms.class_page.read` |  | Get ClassPage. |
| 1855 | `PATCH /v1/comms/class-pages/{classPageId}` | `ClassPageService/UpdateClassPage` | `comms.class_page.update` |  | Update ClassPage. |
| 1856 | `DELETE /v1/comms/class-pages/{classPageId}` | `ClassPageService/DeleteClassPage` | `comms.class_page.delete` |  | Delete ClassPage. |
| 1857 | `GET /v1/comms/class-pages/{classPageId}/attachments` | `ClassPageService/ListClassPageAttachments` | `comms.class_page_attachment.read` |  | List ClassPageAttachments. |
| 1858 | `POST /v1/comms/class-pages/{classPageId}/attachments` | `ClassPageService/CreateClassPageAttachment` | `comms.class_page_attachment.create` |  | Create ClassPageAttachment. |
| 1859 | `GET /v1/comms/class-pages/{classPageId}/attachments/{classPageAttachmentId}` | `ClassPageService/GetClassPageAttachment` | `comms.class_page_attachment.read` |  | Get ClassPageAttachment. |
| 1860 | `PATCH /v1/comms/class-pages/{classPageId}/attachments/{classPageAttachmentId}` | `ClassPageService/UpdateClassPageAttachment` | `comms.class_page_attachment.update` |  | Update ClassPageAttachment. |
| 1861 | `DELETE /v1/comms/class-pages/{classPageId}/attachments/{classPageAttachmentId}` | `ClassPageService/DeleteClassPageAttachment` | `comms.class_page_attachment.delete` |  | Delete ClassPageAttachment. |
| 1862 | `GET /v1/comms/class-pages/{classPageId}/members` | `ClassPageService/ListClassPageMembers` | `comms.class_page_member.read` |  | List ClassPageMembers. |
| 1863 | `POST /v1/comms/class-pages/{classPageId}/members` | `ClassPageService/CreateClassPageMember` | `comms.class_page_member.create` |  | Create ClassPageMember. |
| 1864 | `GET /v1/comms/class-pages/{classPageId}/members/{classPageMemberId}` | `ClassPageService/GetClassPageMember` | `comms.class_page_member.read` |  | Get ClassPageMember. |
| 1865 | `PATCH /v1/comms/class-pages/{classPageId}/members/{classPageMemberId}` | `ClassPageService/UpdateClassPageMember` | `comms.class_page_member.update` |  | Update ClassPageMember. |
| 1866 | `DELETE /v1/comms/class-pages/{classPageId}/members/{classPageMemberId}` | `ClassPageService/DeleteClassPageMember` | `comms.class_page_member.delete` |  | Delete ClassPageMember. |
| 1867 | `GET /v1/comms/posts` | `PostService/ListPosts` | `comms.post.read` |  | List Posts. |
| 1868 | `POST /v1/comms/posts` | `PostService/CreatePost` | `comms.post.create` |  | Create Post. |
| 1869 | `GET /v1/comms/posts/{postId}` | `PostService/GetPost` | `comms.post.read` |  | Get Post. |
| 1870 | `PATCH /v1/comms/posts/{postId}` | `PostService/UpdatePost` | `comms.post.update` |  | Update Post. |
| 1871 | `DELETE /v1/comms/posts/{postId}` | `PostService/DeletePost` | `comms.post.delete` |  | Delete Post. |
| 1872 | `GET /v1/comms/posts/{postId}/audience-classes` | `PostService/ListPostAudienceClasses` | `comms.post_audience_class.read` |  | List PostAudienceClasses. |
| 1873 | `POST /v1/comms/posts/{postId}/audience-classes` | `PostService/CreatePostAudienceClass` | `comms.post_audience_class.create` |  | Create PostAudienceClass. |
| 1874 | `GET /v1/comms/posts/{postId}/audience-classes/{postAudienceClassId}` | `PostService/GetPostAudienceClass` | `comms.post_audience_class.read` |  | Get PostAudienceClass. |
| 1875 | `PATCH /v1/comms/posts/{postId}/audience-classes/{postAudienceClassId}` | `PostService/UpdatePostAudienceClass` | `comms.post_audience_class.update` |  | Update PostAudienceClass. |
| 1876 | `DELETE /v1/comms/posts/{postId}/audience-classes/{postAudienceClassId}` | `PostService/DeletePostAudienceClass` | `comms.post_audience_class.delete` |  | Delete PostAudienceClass. |
| 1877 | `GET /v1/comms/posts/{postId}/reactions` | `PostService/ListPostReactions` | `comms.post_reaction.read` |  | List PostReactions. |
| 1878 | `POST /v1/comms/posts/{postId}/reactions` | `PostService/CreatePostReaction` | `comms.post_reaction.create` |  | Create PostReaction. |
| 1879 | `GET /v1/comms/posts/{postId}/reactions/{postReactionId}` | `PostService/GetPostReaction` | `comms.post_reaction.read` |  | Get PostReaction. |
| 1880 | `PATCH /v1/comms/posts/{postId}/reactions/{postReactionId}` | `PostService/UpdatePostReaction` | `comms.post_reaction.update` |  | Update PostReaction. |
| 1881 | `DELETE /v1/comms/posts/{postId}/reactions/{postReactionId}` | `PostService/DeletePostReaction` | `comms.post_reaction.delete` |  | Delete PostReaction. |
| 1882 | `GET /v1/comms/posts/{postId}/comments` | `PostService/ListPostComments` | `comms.post_comment.read` |  | List PostComments. |
| 1883 | `POST /v1/comms/posts/{postId}/comments` | `PostService/CreatePostComment` | `comms.post_comment.create` |  | Create PostComment. |
| 1884 | `GET /v1/comms/posts/{postId}/comments/{postCommentId}` | `PostService/GetPostComment` | `comms.post_comment.read` |  | Get PostComment. |
| 1885 | `PATCH /v1/comms/posts/{postId}/comments/{postCommentId}` | `PostService/UpdatePostComment` | `comms.post_comment.update` |  | Update PostComment. |
| 1886 | `DELETE /v1/comms/posts/{postId}/comments/{postCommentId}` | `PostService/DeletePostComment` | `comms.post_comment.delete` |  | Delete PostComment. |
| 1887 | `GET /v1/comms/comment-reactions` | `CommentReactionService/ListCommentReactions` | `comms.comment_reaction.read` |  | List CommentReactions. |
| 1888 | `POST /v1/comms/comment-reactions` | `CommentReactionService/CreateCommentReaction` | `comms.comment_reaction.create` |  | Create CommentReaction. |
| 1889 | `GET /v1/comms/comment-reactions/{commentReactionId}` | `CommentReactionService/GetCommentReaction` | `comms.comment_reaction.read` |  | Get CommentReaction. |
| 1890 | `PATCH /v1/comms/comment-reactions/{commentReactionId}` | `CommentReactionService/UpdateCommentReaction` | `comms.comment_reaction.update` |  | Update CommentReaction. |
| 1891 | `DELETE /v1/comms/comment-reactions/{commentReactionId}` | `CommentReactionService/DeleteCommentReaction` | `comms.comment_reaction.delete` |  | Delete CommentReaction. |
| 1892 | `GET /v1/comms/notices/{noticeId}/audiences` | `NoticeService/ListNoticeAudiences` | `comms.notice_audience.read` |  | List NoticeAudiences. |
| 1893 | `POST /v1/comms/notices/{noticeId}/audiences` | `NoticeService/CreateNoticeAudience` | `comms.notice_audience.create` |  | Create NoticeAudience. |
| 1894 | `GET /v1/comms/notices/{noticeId}/audiences/{noticeAudienceId}` | `NoticeService/GetNoticeAudience` | `comms.notice_audience.read` |  | Get NoticeAudience. |
| 1895 | `PATCH /v1/comms/notices/{noticeId}/audiences/{noticeAudienceId}` | `NoticeService/UpdateNoticeAudience` | `comms.notice_audience.update` |  | Update NoticeAudience. |
| 1896 | `DELETE /v1/comms/notices/{noticeId}/audiences/{noticeAudienceId}` | `NoticeService/DeleteNoticeAudience` | `comms.notice_audience.delete` |  | Delete NoticeAudience. |
| 1897 | `GET /v1/comms/notices/{noticeId}/versions` | `NoticeService/ListNoticeVersions` | `comms.notice_version.read` |  | List NoticeVersions. |
| 1898 | `GET /v1/comms/notices/{noticeId}/versions/{noticeVersionId}` | `NoticeService/GetNoticeVersion` | `comms.notice_version.read` |  | Get NoticeVersion. |
| 1899 | `GET /v1/comms/events/{eventId}/institutions` | `EventService/ListEventInstitutions` | `comms.event_institution.read` |  | List EventInstitutions. |
| 1900 | `POST /v1/comms/events/{eventId}/institutions` | `EventService/CreateEventInstitution` | `comms.event_institution.create` |  | Create EventInstitution. |
| 1901 | `GET /v1/comms/events/{eventId}/institutions/{eventInstitutionId}` | `EventService/GetEventInstitution` | `comms.event_institution.read` |  | Get EventInstitution. |
| 1902 | `PATCH /v1/comms/events/{eventId}/institutions/{eventInstitutionId}` | `EventService/UpdateEventInstitution` | `comms.event_institution.update` |  | Update EventInstitution. |
| 1903 | `DELETE /v1/comms/events/{eventId}/institutions/{eventInstitutionId}` | `EventService/DeleteEventInstitution` | `comms.event_institution.delete` |  | Delete EventInstitution. |
| 1904 | `GET /v1/comms/events/{eventId}/occurrences` | `EventService/ListEventOccurrences` | `comms.event_occurrence.read` |  | List EventOccurrences. |
| 1905 | `POST /v1/comms/events/{eventId}/occurrences` | `EventService/CreateEventOccurrence` | `comms.event_occurrence.create` |  | Create EventOccurrence. |
| 1906 | `GET /v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}` | `EventService/GetEventOccurrence` | `comms.event_occurrence.read` |  | Get EventOccurrence. |
| 1907 | `PATCH /v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}` | `EventService/UpdateEventOccurrence` | `comms.event_occurrence.update` |  | Update EventOccurrence. |
| 1908 | `DELETE /v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}` | `EventService/DeleteEventOccurrence` | `comms.event_occurrence.delete` |  | Delete EventOccurrence. |
| 1909 | `GET /v1/comms/events/{eventId}/attendees` | `EventService/ListEventAttendees` | `comms.event_attendee.read` |  | List EventAttendees. |
| 1910 | `POST /v1/comms/events/{eventId}/attendees` | `EventService/CreateEventAttendee` | `comms.event_attendee.create` |  | Create EventAttendee. |
| 1911 | `GET /v1/comms/events/{eventId}/attendees/{eventAttendeeId}` | `EventService/GetEventAttendee` | `comms.event_attendee.read` |  | Get EventAttendee. |
| 1912 | `PATCH /v1/comms/events/{eventId}/attendees/{eventAttendeeId}` | `EventService/UpdateEventAttendee` | `comms.event_attendee.update` |  | Update EventAttendee. |
| 1913 | `DELETE /v1/comms/events/{eventId}/attendees/{eventAttendeeId}` | `EventService/DeleteEventAttendee` | `comms.event_attendee.delete` |  | Delete EventAttendee. |
| 1914 | `GET /v1/comms/calendar-exports` | `CalendarExportService/ListCalendarExports` | `comms.calendar_export.read` |  | List CalendarExports. |
| 1915 | `GET /v1/comms/calendar-exports/{calendarExportId}` | `CalendarExportService/GetCalendarExport` | `comms.calendar_export.read` |  | Get CalendarExport. |
| 1916 | `DELETE /v1/comms/calendar-exports/{calendarExportId}` | `CalendarExportService/DeleteCalendarExport` | `comms.calendar_export.delete` |  | Delete CalendarExport. |
| 1917 | `GET /v1/comms/notification-types` | `NotificationTypeService/ListNotificationTypes` | `comms.notification_type.read` |  | List NotificationTypes. |
| 1918 | `POST /v1/comms/notification-types` | `NotificationTypeService/CreateNotificationType` | `comms.notification_type.create` |  | Create NotificationType. |
| 1919 | `GET /v1/comms/notification-types/{notificationTypeId}` | `NotificationTypeService/GetNotificationType` | `comms.notification_type.read` |  | Get NotificationType. |
| 1920 | `PATCH /v1/comms/notification-types/{notificationTypeId}` | `NotificationTypeService/UpdateNotificationType` | `comms.notification_type.update` |  | Update NotificationType. |
| 1921 | `DELETE /v1/comms/notification-types/{notificationTypeId}` | `NotificationTypeService/DeleteNotificationType` | `comms.notification_type.delete` |  | Delete NotificationType. |
| 1922 | `GET /v1/comms/notification-types/{notificationTypeId}/properties` | `NotificationTypeService/ListNotificationTypeProperties` | `comms.notification_type_property.read` |  | List NotificationTypeProperties. |
| 1923 | `POST /v1/comms/notification-types/{notificationTypeId}/properties` | `NotificationTypeService/CreateNotificationTypeProperty` | `comms.notification_type_property.create` |  | Create NotificationTypeProperty. |
| 1924 | `GET /v1/comms/notification-types/{notificationTypeId}/properties/{notificationTypePropertyId}` | `NotificationTypeService/GetNotificationTypeProperty` | `comms.notification_type_property.read` |  | Get NotificationTypeProperty. |
| 1925 | `PATCH /v1/comms/notification-types/{notificationTypeId}/properties/{notificationTypePropertyId}` | `NotificationTypeService/UpdateNotificationTypeProperty` | `comms.notification_type_property.update` |  | Update NotificationTypeProperty. |
| 1926 | `DELETE /v1/comms/notification-types/{notificationTypeId}/properties/{notificationTypePropertyId}` | `NotificationTypeService/DeleteNotificationTypeProperty` | `comms.notification_type_property.delete` |  | Delete NotificationTypeProperty. |
| 1927 | `GET /v1/comms/notification-types/{notificationTypeId}/rules` | `NotificationTypeService/ListNotificationRules` | `comms.notification_rule.read` |  | List NotificationRules. |
| 1928 | `POST /v1/comms/notification-types/{notificationTypeId}/rules` | `NotificationTypeService/CreateNotificationRule` | `comms.notification_rule.create` |  | Create NotificationRule. |
| 1929 | `GET /v1/comms/notification-types/{notificationTypeId}/rules/{notificationRuleId}` | `NotificationTypeService/GetNotificationRule` | `comms.notification_rule.read` |  | Get NotificationRule. |
| 1930 | `PATCH /v1/comms/notification-types/{notificationTypeId}/rules/{notificationRuleId}` | `NotificationTypeService/UpdateNotificationRule` | `comms.notification_rule.update` |  | Update NotificationRule. |
| 1931 | `DELETE /v1/comms/notification-types/{notificationTypeId}/rules/{notificationRuleId}` | `NotificationTypeService/DeleteNotificationRule` | `comms.notification_rule.delete` |  | Delete NotificationRule. |
| 1932 | `GET /v1/comms/notification-types/{notificationTypeId}/cohort-settings` | `NotificationTypeService/ListNotificationCohortSettings` | `comms.notification_cohort_setting.read` |  | List NotificationCohortSettings. |
| 1933 | `POST /v1/comms/notification-types/{notificationTypeId}/cohort-settings` | `NotificationTypeService/CreateNotificationCohortSetting` | `comms.notification_cohort_setting.create` |  | Create NotificationCohortSetting. |
| 1934 | `GET /v1/comms/notification-types/{notificationTypeId}/cohort-settings/{notificationCohortSettingId}` | `NotificationTypeService/GetNotificationCohortSetting` | `comms.notification_cohort_setting.read` |  | Get NotificationCohortSetting. |
| 1935 | `PATCH /v1/comms/notification-types/{notificationTypeId}/cohort-settings/{notificationCohortSettingId}` | `NotificationTypeService/UpdateNotificationCohortSetting` | `comms.notification_cohort_setting.update` |  | Update NotificationCohortSetting. |
| 1936 | `DELETE /v1/comms/notification-types/{notificationTypeId}/cohort-settings/{notificationCohortSettingId}` | `NotificationTypeService/DeleteNotificationCohortSetting` | `comms.notification_cohort_setting.delete` |  | Delete NotificationCohortSetting. |
| 1937 | `GET /v1/comms/notification-channels` | `NotificationChannelService/ListNotificationChannels` | `comms.notification_channel.read` |  | List NotificationChannels. |
| 1938 | `POST /v1/comms/notification-channels` | `NotificationChannelService/CreateNotificationChannel` | `comms.notification_channel.create` |  | Create NotificationChannel. |
| 1939 | `GET /v1/comms/notification-channels/{notificationChannelId}` | `NotificationChannelService/GetNotificationChannel` | `comms.notification_channel.read` |  | Get NotificationChannel. |
| 1940 | `PATCH /v1/comms/notification-channels/{notificationChannelId}` | `NotificationChannelService/UpdateNotificationChannel` | `comms.notification_channel.update` |  | Update NotificationChannel. |
| 1941 | `DELETE /v1/comms/notification-channels/{notificationChannelId}` | `NotificationChannelService/DeleteNotificationChannel` | `comms.notification_channel.delete` |  | Delete NotificationChannel. |
| 1942 | `GET /v1/comms/notification-channels/{notificationChannelId}/properties` | `NotificationChannelService/ListNotificationChannelProperties` | `comms.notification_channel_property.read` |  | List NotificationChannelProperties. |
| 1943 | `POST /v1/comms/notification-channels/{notificationChannelId}/properties` | `NotificationChannelService/CreateNotificationChannelProperty` | `comms.notification_channel_property.create` |  | Create NotificationChannelProperty. |
| 1944 | `GET /v1/comms/notification-channels/{notificationChannelId}/properties/{notificationChannelPropertyId}` | `NotificationChannelService/GetNotificationChannelProperty` | `comms.notification_channel_property.read` |  | Get NotificationChannelProperty. |
| 1945 | `PATCH /v1/comms/notification-channels/{notificationChannelId}/properties/{notificationChannelPropertyId}` | `NotificationChannelService/UpdateNotificationChannelProperty` | `comms.notification_channel_property.update` |  | Update NotificationChannelProperty. |
| 1946 | `DELETE /v1/comms/notification-channels/{notificationChannelId}/properties/{notificationChannelPropertyId}` | `NotificationChannelService/DeleteNotificationChannelProperty` | `comms.notification_channel_property.delete` |  | Delete NotificationChannelProperty. |
| 1947 | `GET /v1/comms/notification-preference-sets` | `NotificationPreferenceSetService/ListNotificationPreferenceSets` | `comms.notification_preference_set.read` |  | List NotificationPreferenceSets. |
| 1948 | `POST /v1/comms/notification-preference-sets` | `NotificationPreferenceSetService/CreateNotificationPreferenceSet` | `comms.notification_preference_set.create` |  | Create NotificationPreferenceSet. |
| 1949 | `GET /v1/comms/notification-preference-sets/{notificationPreferenceSetId}` | `NotificationPreferenceSetService/GetNotificationPreferenceSet` | `comms.notification_preference_set.read` |  | Get NotificationPreferenceSet. |
| 1950 | `PATCH /v1/comms/notification-preference-sets/{notificationPreferenceSetId}` | `NotificationPreferenceSetService/UpdateNotificationPreferenceSet` | `comms.notification_preference_set.update` |  | Update NotificationPreferenceSet. |
| 1951 | `DELETE /v1/comms/notification-preference-sets/{notificationPreferenceSetId}` | `NotificationPreferenceSetService/DeleteNotificationPreferenceSet` | `comms.notification_preference_set.delete` |  | Delete NotificationPreferenceSet. |
| 1952 | `GET /v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences` | `NotificationPreferenceSetService/ListNotificationPreferences` | `comms.notification_preference.read` |  | List NotificationPreferences. |
| 1953 | `POST /v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences` | `NotificationPreferenceSetService/CreateNotificationPreference` | `comms.notification_preference.create` |  | Create NotificationPreference. |
| 1954 | `GET /v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences/{notificationPreferenceId}` | `NotificationPreferenceSetService/GetNotificationPreference` | `comms.notification_preference.read` |  | Get NotificationPreference. |
| 1955 | `PATCH /v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences/{notificationPreferenceId}` | `NotificationPreferenceSetService/UpdateNotificationPreference` | `comms.notification_preference.update` |  | Update NotificationPreference. |
| 1956 | `DELETE /v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences/{notificationPreferenceId}` | `NotificationPreferenceSetService/DeleteNotificationPreference` | `comms.notification_preference.delete` |  | Delete NotificationPreference. |
| 1957 | `POST /v1/comms/notices/{noticeId}:publish` | `NoticeService/PublishNotice` | `comms.notice.publish_notice` |  | Publish. |
| 1958 | `POST /v1/comms/notices/{noticeId}:unpublish` | `NoticeService/UnpublishNotice` | `comms.notice.unpublish_notice` |  | Unpublish. |
| 1959 | `POST /v1/comms/events/{eventId}:activate` | `EventService/ActivateEvent` | `comms.event.activate_event` |  | Approve / show. |
| 1960 | `POST /v1/comms/events/{eventId}:deactivate` | `EventService/DeactivateEvent` | `comms.event.deactivate_event` |  | Hide. |
| 1961 | `POST /v1/comms/outbound-messages:send` | `OutboundMessageService/SendMessage` | `comms.outbound_message.send_message` |  | Queue SMS / e-mail / push to people, classes, groups or guardians. |
| 1962 | `POST /v1/comms/outbound-messages/{outboundMessageId}:cancel` | `OutboundMessageService/CancelOutboundMessage` | `comms.outbound_message.cancel_outbound_message` |  | Cancel a queued message. |
| 1963 | `POST /v1/comms/outbound-messages/{outboundMessageId}:retry` | `OutboundMessageService/RetryOutboundMessage` | `comms.outbound_message.retry_outbound_message` |  | Retry a failed message. |
| 1964 | `POST /v1/comms/notifications/{inAppNotificationId}:markRead` | `InAppNotificationService/MarkNotificationRead` | `comms.in_app_notification.mark_notification_read` |  | Mark read. |
| 1965 | `POST /v1/comms/notifications:markAllRead` | `InAppNotificationService/MarkAllNotificationsRead` | `comms.in_app_notification.mark_all_notifications_read` |  | Mark all of the caller's notifications read. |
| 1966 | `POST /v1/comms/mailbox-messages:compose` | `MailboxMessageService/ComposeMail` | `comms.mailbox_message.compose_mail` |  | Send internal mail; copies land in recipients' inboxes. |
| 1967 | `POST /v1/comms/mailbox-messages/{mailboxMessageId}:markRead` | `MailboxMessageService/MarkMailRead` | `comms.mailbox_message.mark_mail_read` |  | Mark read. |
| 1968 | `POST /v1/comms/conversations/{conversationId}:markRead` | `ConversationService/MarkConversationRead` | `comms.conversation.mark_conversation_read` |  | Mark all messages read for the caller. |
| 1969 | `POST /v1/comms/messages/{messageId}:hideForMe` | `ConversationService/HideMessageForMe` | `comms.conversation_message.hide_message_for_me` |  | Hide a message for the caller only. |
| 1970 | `POST /v1/comms/feedback-threads/{feedbackThreadId}:close` | `FeedbackThreadService/CloseFeedbackThread` | `comms.feedback_thread.close_feedback_thread` |  | Close. |
| 1971 | `POST /v1/comms/feedback-threads/{feedbackThreadId}:reopen` | `FeedbackThreadService/ReopenFeedbackThread` | `comms.feedback_thread.reopen_feedback_thread` |  | Reopen. |
| 1972 | `POST /v1/comms/feedback-messages/{messageId}:markReviewStatus` | `FeedbackThreadService/MarkFeedbackMessage` | `comms.feedback_message.mark_feedback_message` |  | Mark seen / action taken. |
| 1973 | `POST /v1/comms/audience-groups/{audienceGroupId}:addMembers` | `AudienceGroupService/AddGroupMembers` | `comms.audience_group.add_group_members` |  | Add many members. |
| 1974 | `POST /v1/comms/tasks/{taskId}:complete` | `TaskService/CompleteTask` | `comms.task.complete_task` |  | Complete. |
| 1975 | `POST /v1/comms/tasks/{taskId}:reopen` | `TaskService/ReopenTask` | `comms.task.reopen_task` |  | Reopen. |
| 1976 | `POST /v1/comms/tasks/{taskId}:snooze` | `TaskService/SnoozeTask` | `comms.task.snooze_task` |  | Snooze reminder. |
| 1977 | `POST /v1/comms/tasks/{taskId}:claim` | `TaskService/ClaimTask` | `comms.task.claim_task` |  | Claim an open task. |
| 1978 | `POST /v1/comms/posts/{postId}:submit` | `PostService/SubmitPost` | `comms.post.submit_post` |  | Submit for moderation / publish. |
| 1979 | `POST /v1/comms/posts/{postId}:approve` | `PostService/ApprovePost` | `comms.post.approve_post` |  | Approve and publish. |
| 1980 | `POST /v1/comms/posts/{postId}:cancel` | `PostService/CancelPost` | `comms.post.cancel_post` |  | Withdraw. |
| 1981 | `GET /v1/comms/posts:feed` | `PostService/GetFeed` | `comms.post.get_feed` |  | Personalised feed. |
| 1982 | `POST /v1/comms/class-pages/{classPageId}:follow` | `ClassPageService/FollowClassPage` | `comms.class_page.follow_class_page` |  | Follow or unfollow. |
| 1983 | `POST /v1/comms/notices/{noticeId}:closeReplies` | `NoticeService/CloseNoticeReplies` | `comms.notice.close_notice_replies` |  | Stop accepting replies. |
| 1984 | `POST /v1/comms/notices/{noticeId}:deactivate` | `NoticeService/DeactivateNotice` | `comms.notice.deactivate_notice` |  | Withdraw a notice. |
| 1985 | `POST /v1/comms/notices/{noticeId}:reactivate` | `NoticeService/ReactivateNotice` | `comms.notice.reactivate_notice` |  | Restore a withdrawn notice. |
| 1986 | `GET /v1/comms/me/calendar` | `EventService/GetMyCalendar` | `comms.event.get_my_calendar` |  | Merged calendar of the caller: institution and cohort events, personal events, item due dates, live classes, cohort sessions and holidays. |
| 1987 | `POST /v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}:cancel` | `EventService/CancelEventOccurrence` | `comms.event_occurrence.cancel_event_occurrence` |  | Cancel one occurrence. |
| 1988 | `POST /v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}:complete` | `EventService/CompletePersonalEvent` | `comms.event_occurrence.complete_personal_event` |  | Mark a personal to-do event complete. |
| 1989 | `POST /v1/comms/me/calendar:export` | `CalendarExportService/ExportCalendar` | `comms.calendar_export.export_calendar` |  | Push the caller's calendar items to a connected external calendar (updates and deletions are tracked). |
| 1990 | `POST /v1/comms/notification-channels/{notificationChannelId}:sendTest` | `NotificationChannelService/SendTestMessage` | `comms.notification_channel.send_test_message` |  | Send a test message through the channel configuration. |
| 1991 | `POST /v1/comms/notifications:archive` | `InAppNotificationService/ArchiveNotifications` | `comms.in_app_notification.archive_notifications` |  | Hide notifications from the caller's inbox (archived_at); they stay in the audit history. |
| 1992 | `GET /v1/comms/operations` | `OperationService/ListOperations` | `comms.operation.read` |  | Long-running operations of this module started by the caller. |
| 1993 | `GET /v1/comms/operations/{operationId}` | `OperationService/GetOperation` | `comms.operation.read` |  | Status of a long-running operation started in this module. |
| 1994 | `POST /v1/comms/operations/{operationId}:cancel` | `OperationService/CancelOperation` | `comms.operation.cancel_operation` |  | Request cancellation of a running operation. |

## Front Office (`frontoffice`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 1995 | `GET /v1/frontoffice/visitor-logs` | `VisitorLogService/ListVisitorLogs` | `frontoffice.visitor_log.read` |  | List VisitorLogs. |
| 1996 | `POST /v1/frontoffice/visitor-logs` | `VisitorLogService/CreateVisitorLog` | `frontoffice.visitor_log.create` |  | Create VisitorLog. |
| 1997 | `GET /v1/frontoffice/visitor-logs/{visitorLogId}` | `VisitorLogService/GetVisitorLog` | `frontoffice.visitor_log.read` |  | Get VisitorLog. |
| 1998 | `PATCH /v1/frontoffice/visitor-logs/{visitorLogId}` | `VisitorLogService/UpdateVisitorLog` | `frontoffice.visitor_log.update` |  | Update VisitorLog. |
| 1999 | `DELETE /v1/frontoffice/visitor-logs/{visitorLogId}` | `VisitorLogService/DeleteVisitorLog` | `frontoffice.visitor_log.delete` |  | Delete VisitorLog. |
| 2000 | `GET /v1/frontoffice/call-logs` | `CallLogService/ListCallLogs` | `frontoffice.call_log.read` |  | List CallLogs. |
| 2001 | `POST /v1/frontoffice/call-logs` | `CallLogService/CreateCallLog` | `frontoffice.call_log.create` |  | Create CallLog. |
| 2002 | `GET /v1/frontoffice/call-logs/{callLogId}` | `CallLogService/GetCallLog` | `frontoffice.call_log.read` |  | Get CallLog. |
| 2003 | `PATCH /v1/frontoffice/call-logs/{callLogId}` | `CallLogService/UpdateCallLog` | `frontoffice.call_log.update` |  | Update CallLog. |
| 2004 | `DELETE /v1/frontoffice/call-logs/{callLogId}` | `CallLogService/DeleteCallLog` | `frontoffice.call_log.delete` |  | Delete CallLog. |
| 2005 | `GET /v1/frontoffice/postal-records` | `PostalRecordService/ListPostalRecords` | `frontoffice.postal_record.read` |  | List PostalRecords. |
| 2006 | `POST /v1/frontoffice/postal-records` | `PostalRecordService/CreatePostalRecord` | `frontoffice.postal_record.create` |  | Create PostalRecord. |
| 2007 | `GET /v1/frontoffice/postal-records/{postalRecordId}` | `PostalRecordService/GetPostalRecord` | `frontoffice.postal_record.read` |  | Get PostalRecord. |
| 2008 | `PATCH /v1/frontoffice/postal-records/{postalRecordId}` | `PostalRecordService/UpdatePostalRecord` | `frontoffice.postal_record.update` |  | Update PostalRecord. |
| 2009 | `DELETE /v1/frontoffice/postal-records/{postalRecordId}` | `PostalRecordService/DeletePostalRecord` | `frontoffice.postal_record.delete` |  | Delete PostalRecord. |
| 2010 | `GET /v1/frontoffice/directory-contacts` | `DirectoryContactService/ListDirectoryContacts` | `frontoffice.directory_contact.read` |  | List DirectoryContacts. |
| 2011 | `POST /v1/frontoffice/directory-contacts` | `DirectoryContactService/CreateDirectoryContact` | `frontoffice.directory_contact.create` |  | Create DirectoryContact. |
| 2012 | `GET /v1/frontoffice/directory-contacts/{directoryContactId}` | `DirectoryContactService/GetDirectoryContact` | `frontoffice.directory_contact.read` |  | Get DirectoryContact. |
| 2013 | `PATCH /v1/frontoffice/directory-contacts/{directoryContactId}` | `DirectoryContactService/UpdateDirectoryContact` | `frontoffice.directory_contact.update` |  | Update DirectoryContact. |
| 2014 | `DELETE /v1/frontoffice/directory-contacts/{directoryContactId}` | `DirectoryContactService/DeleteDirectoryContact` | `frontoffice.directory_contact.delete` |  | Delete DirectoryContact. |
| 2015 | `POST /v1/frontoffice/visitor-logs/{visitorLogId}:checkOut` | `VisitorLogService/CheckOutVisitor` | `frontoffice.visitor_log.check_out_visitor` |  | Record exit time. |

## Careers (`careers`)

| # | REST | gRPC | Permission | Public | Description |
|---|---|---|---|---|---|
| 2016 | `GET /v1/careers/function-areas` | `FunctionAreaService/ListFunctionAreas` | `careers.function_area.read` |  | List FunctionAreas. |
| 2017 | `POST /v1/careers/function-areas` | `FunctionAreaService/CreateFunctionArea` | `careers.function_area.create` |  | Create FunctionArea. |
| 2018 | `GET /v1/careers/function-areas/{functionAreaId}` | `FunctionAreaService/GetFunctionArea` | `careers.function_area.read` |  | Get FunctionArea. |
| 2019 | `PATCH /v1/careers/function-areas/{functionAreaId}` | `FunctionAreaService/UpdateFunctionArea` | `careers.function_area.update` |  | Update FunctionArea. |
| 2020 | `DELETE /v1/careers/function-areas/{functionAreaId}` | `FunctionAreaService/DeleteFunctionArea` | `careers.function_area.delete` |  | Delete FunctionArea. |
| 2021 | `GET /v1/careers/industries` | `IndustryService/ListIndustries` | `careers.industry.read` |  | List Industries. |
| 2022 | `POST /v1/careers/industries` | `IndustryService/CreateIndustry` | `careers.industry.create` |  | Create Industry. |
| 2023 | `GET /v1/careers/industries/{industryId}` | `IndustryService/GetIndustry` | `careers.industry.read` |  | Get Industry. |
| 2024 | `PATCH /v1/careers/industries/{industryId}` | `IndustryService/UpdateIndustry` | `careers.industry.update` |  | Update Industry. |
| 2025 | `DELETE /v1/careers/industries/{industryId}` | `IndustryService/DeleteIndustry` | `careers.industry.delete` |  | Delete Industry. |
| 2026 | `GET /v1/careers/job-openings` | `JobOpeningService/ListJobOpenings` | `careers.job_opening.read` | Public | List JobOpenings. |
| 2027 | `POST /v1/careers/job-openings` | `JobOpeningService/CreateJobOpening` | `careers.job_opening.create` |  | Create JobOpening. |
| 2028 | `GET /v1/careers/job-openings/{jobOpeningId}` | `JobOpeningService/GetJobOpening` | `careers.job_opening.read` | Public | Get JobOpening. |
| 2029 | `PATCH /v1/careers/job-openings/{jobOpeningId}` | `JobOpeningService/UpdateJobOpening` | `careers.job_opening.update` |  | Update JobOpening. |
| 2030 | `DELETE /v1/careers/job-openings/{jobOpeningId}` | `JobOpeningService/DeleteJobOpening` | `careers.job_opening.delete` |  | Delete JobOpening. |
| 2031 | `GET /v1/careers/job-openings/{jobOpeningId}/applications` | `JobOpeningService/ListJobApplications` | `careers.job_application.read` |  | List JobApplications. |
| 2032 | `POST /v1/careers/job-openings/{jobOpeningId}/applications` | `JobOpeningService/CreateJobApplication` | `careers.job_application.create` |  | Create JobApplication. |
| 2033 | `GET /v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}` | `JobOpeningService/GetJobApplication` | `careers.job_application.read` |  | Get JobApplication. |
| 2034 | `PATCH /v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}` | `JobOpeningService/UpdateJobApplication` | `careers.job_application.update` |  | Update JobApplication. |
| 2035 | `DELETE /v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}` | `JobOpeningService/DeleteJobApplication` | `careers.job_application.delete` |  | Delete JobApplication. |
| 2036 | `GET /v1/careers/candidate-preferences` | `CandidatePreferenceService/ListCandidatePreferences` | `careers.candidate_preference.read` |  | List CandidatePreferences. |
| 2037 | `POST /v1/careers/candidate-preferences` | `CandidatePreferenceService/CreateCandidatePreference` | `careers.candidate_preference.create` |  | Create CandidatePreference. |
| 2038 | `GET /v1/careers/candidate-preferences/{candidatePreferenceId}` | `CandidatePreferenceService/GetCandidatePreference` | `careers.candidate_preference.read` |  | Get CandidatePreference. |
| 2039 | `PATCH /v1/careers/candidate-preferences/{candidatePreferenceId}` | `CandidatePreferenceService/UpdateCandidatePreference` | `careers.candidate_preference.update` |  | Update CandidatePreference. |
| 2040 | `DELETE /v1/careers/candidate-preferences/{candidatePreferenceId}` | `CandidatePreferenceService/DeleteCandidatePreference` | `careers.candidate_preference.delete` |  | Delete CandidatePreference. |
| 2041 | `GET /v1/careers/candidate-preferences/{candidatePreferenceId}/functions` | `CandidatePreferenceService/ListCandidatePreferredFunctions` | `careers.candidate_preferred_function.read` |  | List CandidatePreferredFunctions. |
| 2042 | `POST /v1/careers/candidate-preferences/{candidatePreferenceId}/functions` | `CandidatePreferenceService/CreateCandidatePreferredFunction` | `careers.candidate_preferred_function.create` |  | Create CandidatePreferredFunction. |
| 2043 | `GET /v1/careers/candidate-preferences/{candidatePreferenceId}/functions/{candidatePreferredFunctionId}` | `CandidatePreferenceService/GetCandidatePreferredFunction` | `careers.candidate_preferred_function.read` |  | Get CandidatePreferredFunction. |
| 2044 | `PATCH /v1/careers/candidate-preferences/{candidatePreferenceId}/functions/{candidatePreferredFunctionId}` | `CandidatePreferenceService/UpdateCandidatePreferredFunction` | `careers.candidate_preferred_function.update` |  | Update CandidatePreferredFunction. |
| 2045 | `DELETE /v1/careers/candidate-preferences/{candidatePreferenceId}/functions/{candidatePreferredFunctionId}` | `CandidatePreferenceService/DeleteCandidatePreferredFunction` | `careers.candidate_preferred_function.delete` |  | Delete CandidatePreferredFunction. |
| 2046 | `GET /v1/careers/candidate-preferences/{candidatePreferenceId}/industries` | `CandidatePreferenceService/ListCandidatePreferredIndustries` | `careers.candidate_preferred_industry.read` |  | List CandidatePreferredIndustries. |
| 2047 | `POST /v1/careers/candidate-preferences/{candidatePreferenceId}/industries` | `CandidatePreferenceService/CreateCandidatePreferredIndustry` | `careers.candidate_preferred_industry.create` |  | Create CandidatePreferredIndustry. |
| 2048 | `GET /v1/careers/candidate-preferences/{candidatePreferenceId}/industries/{candidatePreferredIndustryId}` | `CandidatePreferenceService/GetCandidatePreferredIndustry` | `careers.candidate_preferred_industry.read` |  | Get CandidatePreferredIndustry. |
| 2049 | `PATCH /v1/careers/candidate-preferences/{candidatePreferenceId}/industries/{candidatePreferredIndustryId}` | `CandidatePreferenceService/UpdateCandidatePreferredIndustry` | `careers.candidate_preferred_industry.update` |  | Update CandidatePreferredIndustry. |
| 2050 | `DELETE /v1/careers/candidate-preferences/{candidatePreferenceId}/industries/{candidatePreferredIndustryId}` | `CandidatePreferenceService/DeleteCandidatePreferredIndustry` | `careers.candidate_preferred_industry.delete` |  | Delete CandidatePreferredIndustry. |
| 2051 | `POST /v1/careers/job-openings/{jobOpeningId}:close` | `JobOpeningService/CloseJobOpening` | `careers.job_opening.close_job_opening` |  | Close. |
| 2052 | `POST /v1/careers/job-openings/{jobOpeningId}:reopen` | `JobOpeningService/ReopenJobOpening` | `careers.job_opening.reopen_job_opening` |  | Reopen. |
| 2053 | `POST /v1/careers/job-openings/{jobOpeningId}:report` | `JobOpeningService/ReportJobOpening` | `careers.job_opening.report_job_opening` |  | Report a listing to moderators. |
| 2054 | `POST /v1/careers/job-applications/{applicationId}:email` | `JobOpeningService/EmailApplicant` | `careers.job_application.email_applicant` |  | E-mail the applicant. |
| 2055 | `POST /v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}:setStatus` | `JobOpeningService/UpdateApplicationStatus` | `careers.job_application.update_application_status` |  | Shortlist / reject / hire. |

