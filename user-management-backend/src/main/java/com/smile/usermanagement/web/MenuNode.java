package com.smile.usermanagement.web;

import com.smile.usermanagement.entity.Menu;
import java.util.ArrayList;
import java.util.List;

public class MenuNode {

    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String icon;
    private String permCode;
    private String type;
    private Integer sortNo;
    private Integer status;
    private List<MenuNode> children = new ArrayList<>();

    public static MenuNode from(Menu menu) {
        MenuNode node = new MenuNode();
        node.setId(menu.getId());
        node.setParentId(menu.getParentId());
        node.setName(menu.getName());
        node.setPath(menu.getPath());
        node.setIcon(menu.getIcon());
        node.setPermCode(menu.getPermCode());
        node.setType(menu.getType());
        node.setSortNo(menu.getSortNo());
        node.setStatus(menu.getStatus());
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPermCode() {
        return permCode;
    }

    public void setPermCode(String permCode) {
        this.permCode = permCode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public List<MenuNode> getChildren() {
        return children;
    }

    public void setChildren(List<MenuNode> children) {
        this.children = children;
    }
}

