package cn.cmcc.diseasemonitor.service;

import cn.cmcc.diseasemonitor.entity.User;
import cn.cmcc.diseasemonitor.util.constant.SmsType;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.stream.DoubleStream;

public interface UserService {

    /**
     * 实验室人员登录, 返回登录是否成功,
     *
     * @param username   用户名
     * @param password   密码
     * @param verifyCode 验证码
     * @return token 没有用户返回-3，密码错误返回-2，没有权限返回-1，登录成功返回userId,
     * 未知错误返回-100
     */
    Integer login(String username, String password, String verifyCode, String ip);

    /**
     * 获取登录用户信息和电话号码
     *
     * @param token :
     * @return: cn.cmcc.diseasemonitor.entity.User
     */
    Optional<Map<String, Object>> findUserInfoAndPhone(String token);

    /**
     * 获取登录用户UserId
     *
     * @param token :
     * @return: java.util.Optional<java.lang.Integer>
     */
    Optional<Integer> findUserIdByToken(String token);

    /**
     * 获取登录用户
     *
     * @param token :
     * @return: java.util.Optional
     */
    Optional<User> findUserByToken(String token);

    /**
     * 查找用户信息
     *
     * @param id : 用户ID
     * @return: cn.cmcc.diseasemonitor.entity.User
     */
    Optional<User> findById(Integer id);

    /**
     * 修改用户名
     *
     * @param token    :
     * @param username : 用户名
     * @return: java.util.Optional<java.lang.Integer> 1成功 null token无效
     */
    Optional<Integer> updateUserName(String token, String username);

    /**
     * 修改密码
     *
     * @param token       :
     * @param newPassword : 新密码
     * @return: java.util.Optional<java.lang.Integer> 1 成功 -1 原密码错误 null token无效
     */
    Optional<Integer> updatePassword(String token, String newPassword);

    /**
     * 修改联系手机
     *
     * @param token :
     * @param phone :
     * @return: java.util.Optional<java.lang.Integer>
     */
    Optional<Integer> updatePhone(String token, String phone, String phoneCode);

    /**
     * 返回当前Ip登录是否需要验证码
     *
     * @param request
     * @return
     */
    Boolean needVerifyCode(HttpServletRequest request);

    /**
     * 1. 先验证手机号对应的用户是否存在且是否唯一
     * 2. 生成短信验证码存到redis,格式 < 手机号, 短信验证码 >
     *
     * @param ipAddr
     * @param verifyCode
     * @param phone
     * @return
     */
    Map<String, String> generateSMScode(String ipAddr, String verifyCode, String phone, String token);

    /**
     * 为新手机生成验证码，传入新手机号码，在redis中生成新手机号和验证码的相关记录
     * 在绑定新手机的时候有用
     *
     * @param ipAddr
     * @param phone
     * @return
     */
    Map<String, String> generateSMScodeForNewPhone(String ipAddr, String phone, SmsType smsType);

    /**
     * 修改密码
     *
     * @param phoneCode
     * @param newPwd
     * @return
     */
    Map<String, String> changePwd(String phoneCode, String newPwd);

    /**
     * 传手机号码和手机验证码进行登录
     *
     * @param phone
     * @param phoneCode
     * @return
     */
    Integer loginWithPhone(String phone, String phoneCode, HttpServletRequest request);

    Optional<Integer> updateNickName(String token, String nickName);

}
