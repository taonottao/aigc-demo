package com.smile.usermanagement.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMenuMapper {

    @Select("select menu_id from sys_role_menu where role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Select({
        "<script>",
        "select distinct menu_id from sys_role_menu where role_id in",
        "<foreach item='roleId' collection='roleIds' open='(' separator=',' close=')'>",
        "#{roleId}",
        "</foreach>",
        "</script>"
    })
    List<Long> selectMenuIdsByRoleIds(@Param("roleIds") List<Long> roleIds);

    @Delete("delete from sys_role_menu where role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Insert("insert into sys_role_menu(role_id, menu_id) values(#{roleId}, #{menuId})")
    int insert(@Param("roleId") Long roleId, @Param("menuId") Long menuId);
}
