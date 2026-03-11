package com.smile.usermanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smile.usermanagement.entity.Org;
import com.smile.usermanagement.web.OrgNode;
import java.util.List;
import java.util.Set;

public interface OrgService extends IService<Org> {

    List<OrgNode> getOrgTree();

    Set<Long> getDescendantOrgIds(Long orgId);

    Org createOrg(Org org);

    Org updateOrg(Long id, Org org);

    void deleteOrg(Long id);
}

