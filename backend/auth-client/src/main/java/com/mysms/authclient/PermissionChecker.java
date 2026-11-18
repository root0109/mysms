package com.mysms.authclient;

/** Answers "may this caller perform the operation guarded by this x-permission code?". */
public interface PermissionChecker {

    boolean isPermitted(AuthContext caller, String permission);
}
