package com.smile.usermanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smile.usermanagement.entity.UserRole;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Select("""
        <script>
        SELECT user_id, role_id
        FROM sys_user_role
        WHERE user_id IN
        <foreach collection='userIds' item='id' open='(' separator=',' close=')'>
            #{id}
        </foreach>
        </script>
        """)
    List<UserRole> selectByUserIds(@Param("userIds") List<Long> userIds);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}
