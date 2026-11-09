package com.mysms.authclient;

/**
 * Checks the operation's permission code against the effective permissions the issuer put in the token. Data-level
 * rules (a guardian sees only their children...) are applied by the business service on top of this check.
 */
public final class ClaimsPermissionChecker implements PermissionChecker {

    @Override
    public boolean isPermitted(AuthContext caller, String permission) {
        return caller.hasPermission(permission);
    }
}
