package com.smile.usermanagement.service;

import java.util.Set;

public interface DataScopeService {

    String MODULE_USER = "USER";

    enum Scope {
        ALL,
        ORG_AND_CHILDREN,
        ORG_ONLY,
        SELF_ONLY
    }

    Scope resolveUserModuleScope(Long userId);

    Set<Long> resolveAccessibleOrgIds(Long userId);
}

