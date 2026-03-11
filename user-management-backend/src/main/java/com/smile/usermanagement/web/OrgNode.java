package com.smile.usermanagement.web;

import com.smile.usermanagement.entity.Org;
import java.util.ArrayList;
import java.util.List;

public class OrgNode {

    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private String leader;
    private String description;
    private Integer sortNo;
    private Integer status;
    private List<OrgNode> children = new ArrayList<>();

    public static OrgNode from(Org org) {
        OrgNode node = new OrgNode();
        node.setId(org.getId());
        node.setParentId(org.getParentId());
        node.setName(org.getName());
        node.setCode(org.getCode());
        node.setLeader(org.getLeader());
        node.setDescription(org.getDescription());
        node.setSortNo(org.getSortNo());
        node.setStatus(org.getStatus());
        return node;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<OrgNode> getChildren() {
        return children;
    }

    public void setChildren(List<OrgNode> children) {
        this.children = children;
    }
}

