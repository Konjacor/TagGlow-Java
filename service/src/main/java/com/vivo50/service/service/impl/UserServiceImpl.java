package com.vivo50.service.service.impl;

import com.vivo50.service.entity.User;
import com.vivo50.service.mapper.UserMapper;
import com.vivo50.service.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author CanoeLike
 * @since 2025-06-04
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

}
