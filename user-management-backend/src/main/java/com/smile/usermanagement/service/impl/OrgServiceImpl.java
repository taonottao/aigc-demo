package com.smile.usermanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smile.usermanagement.entity.Org;
import com.smile.usermanagement.mapper.OrgMapper;
import com.smile.usermanagement.mapper.UserMapper;
import com.smile.usermanagement.service.OrgService;
import com.smile.usermanagement.web.OrgNode;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrgServiceImpl extends ServiceImpl<OrgMapper, Org> implements OrgService {

    private final UserMapper userMapper;

    public OrgServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<OrgNode> getOrgTree() {
        List<Org> orgs = list(new LambdaQueryWrapper<Org>()
            .orderByAsc(Org::getSortNo)
            .orderByAsc(Org::getId));

        Map<Long, OrgNode> map = new HashMap<>();
        for (Org org : orgs) {
            map.put(org.getId(), OrgNode.from(org));
        }

        List<OrgNode> roots = new ArrayList<>();
        for (Org org : orgs) {
            OrgNode node = map.get(org.getId());
            Long parentId = org.getParentId() == null ? 0L : org.getParentId();
            if (parentId == 0L || !map.containsKey(parentId)) {
                roots.add(node);
                continue;
            }
            map.get(parentId).getChildren().add(node);
        }

        sortTree(roots);
        return roots;
    }

    @Override
    public Set<Long> getDescendantOrgIds(Long orgId) {
        if (orgId == null) {
            return Set.of();
        }

        List<Org> orgs = list();
        Map<Long, List<Long>> children = new HashMap<>();
        for (Org org : orgs) {
            Long parent = org.getParentId() == null ? 0L : org.getParentId();
            children.computeIfAbsent(parent, k -> new ArrayList<>()).add(org.getId());
        }

        Set<Long> result = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(orgId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (!result.add(current)) {
                continue;
            }
            List<Long> next = children.get(current);
            if (next != null) {
                queue.addAll(next);
            }
        }
        return result;
    }

    @Override
    public Org createOrg(Org org) {
        validateCodeUnique(org.getCode(), null);
        if (!StringUtils.hasText(org.getName())) {
            throw new IllegalArgumentException("Org name is required");
        }
        LocalDateTime now = LocalDateTime.now();
        org.setCreatedAt(now);
        org.setUpdatedAt(now);
        if (org.getParentId() == null) {
            org.setParentId(0L);
        }
        if (org.getSortNo() == null) {
            org.setSortNo(0);
        }
        if (org.getStatus() == null) {
            org.setStatus(1);
        }
        save(org);
        return org;
    }

    @Override
    public Org updateOrg(Long id, Org org) {
        Org existing = getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Org not found: " + id);
        }
        validateCodeUnique(org.getCode(), id);

        existing.setName(org.getName());
        existing.setCode(org.getCode());
        existing.setLeader(org.getLeader());
        existing.setDescription(org.getDescription());
        existing.setParentId(org.getParentId() == null ? 0L : org.getParentId());
        existing.setSortNo(org.getSortNo());
        existing.setStatus(org.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        updateById(existing);
        return existing;
    }

    @Override
    public void deleteOrg(Long id) {
        long childrenCount = count(new LambdaQueryWrapper<Org>().eq(Org::getParentId, id));
        if (childrenCount > 0) {
            throw new IllegalArgumentException("Org has children; delete children first");
        }
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<com.smile.usermanagement.entity.User>()
            .eq(com.smile.usermanagement.entity.User::getOrgId, id));
        if (userCount > 0) {
            throw new IllegalArgumentException("Org has users; move users first");
        }
        boolean removed = removeById(id);
        if (!removed) {
            throw new IllegalArgumentException("Org not found: " + id);
        }
    }

    private void validateCodeUnique(String code, Long excludeId) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Org code is required");
        }
        LambdaQueryWrapper<Org> wrapper = new LambdaQueryWrapper<Org>().eq(Org::getCode, code);
        if (excludeId != null) {
            wrapper.ne(Org::getId, excludeId);
        }
        Long count = count(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("Org code already exists: " + code);
        }
    }

    private void sortTree(List<OrgNode> nodes) {
        nodes.sort(Comparator.comparingInt(n -> n.getSortNo() == null ? 0 : n.getSortNo()));
        for (OrgNode node : nodes) {
            sortTree(node.getChildren());
        }
    }
}

