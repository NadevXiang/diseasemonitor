package cn.cmcc.diseasemonitor.service.impl;

import cn.cmcc.diseasemonitor.repository.LaboratoryRepository;
import cn.cmcc.diseasemonitor.util.*;
import cn.cmcc.diseasemonitor.util.constant.SmsType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.cmcc.diseasemonitor.entity.User;
import cn.cmcc.diseasemonitor.service.UserService;
import cn.cmcc.diseasemonitor.repository.UserRepository;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository resp;

    @Autowired
    LaboratoryRepository laboratoryRepository;

    @Override
    public Integer login(String username, String password, String verifyCode, String ip) {
        return loginCommon(username, password, verifyCode, ip, false);
    }


    @Override
    public Optional<Map<String, Object>> findUserInfoAndPhone(String token) {
        return findUserIdByToken(token).map(resp::findUserInfoAndPhone).orElse(Optional.empty());
    }


    @Override
    public Optional<Integer> findUserIdByToken(String token) {
        /* 获取用户ID */
        Optional<String> idOpt = Optional.ofNullable(
                RedisUtil.getInstance().readDataFromRedis(token));
        return idOpt.map((v) -> Optional.of(Integer.valueOf(v))).orElse(Optional.empty());
    }


    @Override
    public Optional<User> findUserByToken(String token) {
        return this.findUserIdByToken(token).map(resp::findById).orElse(Optional.empty());
    }


    @Override
    public Optional findById(Integer id) {
        return resp.findById(id);
    }

    @Override
    public Optional<Integer> updateUserName(String token, String username) {
        return this.findUserByToken(token).map((v) -> {
            v.setUserName(username);
            resp.save(v);
            return Optional.of(1);
        }).orElse(Optional.empty());
    }

    @Override
    public Optional<Integer> updatePassword(String token, String newPassword) {
        String newPasswordMD5 = MD5Util.trueMd5(newPassword);
        return this.findUserByToken(token).map((v) -> {
            if (null != v) {
                v.setPasswd(newPasswordMD5);
                resp.save(v);
                return Optional.of(1);
            }
            return Optional.of(-1);
        }).orElse(Optional.empty());
    }

    @Override
    public Optional<Integer> updatePhone(String token, String phone, String phoneCode) {
        String phoneFromRedis = RedisUtil.getInstance().readDataFromRedis(phoneCode);
        if (phoneFromRedis == null || phone == null || !phoneFromRedis.equals(phone)) {
            return Optional.empty();
        }
        return this.findUserIdByToken(token).map((v) -> {
            return resp.findById(v).map(
                    (k) -> {
                        k.setPhone(phone);
                        resp.save(k);
                        return Optional.of(1);
                    }
            ).orElse(Optional.empty());
        }).orElse(Optional.empty());
    }

    @Override
    public Boolean needVerifyCode(HttpServletRequest request) {
        return isNeedVerifyCode(IpUtils.getIpAddr(request));
    }

    //这个ipAddr没啥用
    @Override
    public Map<String, String> generateSMScodeForNewPhone(String ipAddr, String phone, SmsType smsType) {
        log.info("generateSMScodeForNewPhone,操作者IP：" + ipAddr);
        Map<String, String> rs = new HashMap<>();

        // 检查用户是否存在
        if (resp.findByPhone(phone) == null) {
            rs.put("STATUS", "FAILURE");
            rs.put("ERROR", "该手机号尚未被绑定");
            return rs;
        }

        if (null != RedisUtil.getInstance().readDataFromRedis(ipAddr + "phoneCode")) {
            //这里应该报错，你的操作太快了，1分钟冷却时间未到。
            rs.put("STATUS", "FAILURE");
            rs.put("ERROR", "你的操作太快了，还没到一分钟");
            return rs;
        }

        //生成并发送到手机号码
        String code = MD5Util.randomNumsStr(4);
        boolean result = SmsUtil.send(code, smsType.getName(), phone);
        if (result) {
            // 验证码有效时间30分钟
            RedisUtil.getInstance().setDataToRedis(code, phone, 30);
            // flag 用于判断是否 发送验证码满 1 分钟了
            RedisUtil.getInstance().setDataToRedis(ipAddr + "phoneCode", code, 1);
            rs.put("STATUS", "SUCCESS");
        } else {
            rs.put("STATUS", "FAILURE");
        }
        return rs;
    }

    @Override
    public Map<String, String> generateSMScode(String ipAddr,
                                               String verifyCode,
                                               String phone,
                                               String token) {
        String tokenUserId = RedisUtil.getInstance().readDataFromRedis(token);
        // 先验证图片验证码, 防止恶意发送手机验证码
        Map<String, String> rs = new HashMap<>();

        if (resp.findByPhone(phone) == null) {
            rs.put("STATUS", "FAILURE");
            rs.put("ERROR", "该手机号尚未被绑定");
            return rs;
        }

        String verify = RedisUtil.getInstance().readDataFromRedis(ipAddr + "verifycode");
        if (null == tokenUserId) {
            if (verifyCode == null || !verifyCode.toLowerCase().equals(verify.toLowerCase())) {
                rs.put("STATUS", "FAILURE");
                rs.put("ERROR", "请输入正确的图片验证码");
                return rs;
            }
            // 清除验证码
            RedisUtil.getInstance().setDataToRedis(ipAddr + "verifycode", "", 30);
        }


        //模拟生成手机验证码
        String code = MD5Util.randomNumsStr(4);

        boolean result = SmsUtil.send(code, SmsType.FORGOT_PASSWORD.getName(), phone);
        if (result) {
            // 验证码有效时间30分钟
            RedisUtil.getInstance().setDataToRedis(code, phone, 30);
            rs.put("STATUS", "SUCCESS");
        } else {
            rs.put("STATUS", "FAILURE");
            rs.put("ERROR", "发送失败，短信接口错误");
        }
        return rs;

    }

    @Override
    public Map<String, String> changePwd(String phoneCode, String newPwd) {
        Map<String, String> rs = new HashMap<>();
        String phone = RedisUtil.getInstance().readDataFromRedis(phoneCode);
        if (null == phone) {
            rs.put("ERROR", "验证码输入错误,没有找到验证码");
            return rs;
        }
        //此处理论上已经找到用户存在了，不需要try catch了吧,,嘿嘿
        User user = resp.findByPhone(phone);
        user.setPasswd(MD5Util.trueMd5(newPwd));
        resp.save(user);
        rs.put("SUCCESS", "找回密码成功");
        return rs;
    }

    @Override
    public Integer loginWithPhone(String phone, String phoneCode, HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        User user = resp.findByPhone(phone);
        if (null == user) {
            return Constant.NO_USER;
        }
        //校验手机验证码
        String phoneFromRedis = RedisUtil.getInstance().readDataFromRedis(phoneCode);
        if (phoneFromRedis != null && phone != null && phoneFromRedis.equals(phone))
            return loginCommon(user.getUserName(), user.getPasswd(), null, ip, true);
        else
            return Constant.PHONE_WRONG;
    }

    @Override
    public Optional<Integer> updateNickName(String token, String nickName) {
        return this.findUserByToken(token).map((v) -> {
            v.setNickname(nickName);
            resp.save(v);
            return Optional.of(1);
        }).orElse(Optional.empty());
    }

    /**
     * 如果登录错误次数大于等于3次就要返回需要验证码
     *
     * @param ip
     * @return
     */
    private boolean isNeedVerifyCode(String ip) {
        String timeStr = RedisUtil.getInstance().readDataFromRedis(ip);
        if (null == timeStr) {
            return false;
        }
        int time = Integer.parseInt(timeStr);
        if (time >= 3) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 增加一次登录出错记录在redis中
     *
     * @param ip
     */
    private void addWrongLoginTime(String ip) {
        if (null != RedisUtil.getInstance().readDataFromRedis(ip)) {
            Integer time = Integer.parseInt(RedisUtil.getInstance().readDataFromRedis(ip));
            Integer time2 = time + 1;
            RedisUtil.getInstance().setDataToRedis(ip, time2.toString(), 15);
        } else {
            RedisUtil.getInstance().setDataToRedis(ip, "1", 15);
        }
        System.out.println("redisIp : " + ip + ",value: " + RedisUtil.getInstance().readDataFromRedis(ip));
    }

    /**
     * 清除登录错误的次数记录
     *
     * @param ip
     */
    private void clearWrongLoginTime(String ip) {
        RedisUtil.getInstance().setDataToRedis(ip, "0", 15);
    }

    /**
     * 登录公共方法
     *
     * @return
     */
    private Integer loginCommon(String username, String password, String verifyCode, String ip, Boolean isPhone) {
        // 检测是否需要验证码
        // time 这里是次数  不是时间
        if (!isPhone) {
            String timeStr = RedisUtil.getInstance().readDataFromRedis(ip);
            if (null == timeStr) {
                //NULL
            } else {
                Integer time = Integer.parseInt(timeStr);
                if (null != time && time >= 3) {
                    // 需要验证码 生成的验证码存到 redis  key: ip + verifycode,   value: verifycode
                    String verifyCode2 = RedisUtil.getInstance().readDataFromRedis(ip + "verifycode");
                    if (null == verifyCode2 || verifyCode == null) {
                        return Constant.VERIFYCODE_NONE;
                    }
                    if (!verifyCode.toLowerCase().equals(verifyCode2.toLowerCase())) {
                        return Constant.VERIFYCODE_ERROR;
                    }
                }
            }
        }
        // 验证码正确 下一步删除验证码 继续登录
        RedisUtil.getInstance().setDataToRedis(ip + "verifycode", "", 30);
        //User user = resp.findByUserName(username);

        /**
         * 前端说是手机号码 + 密码登录, 而不是用户名 + 密码登录
         */
        User user = resp.findByPhone(username);

        if (null == user) {
            // 没有找到用户
            addWrongLoginTime(ip);
            return Constant.NO_USER;
        }
        password = isPhone ? password.toUpperCase() : MD5Util.trueMd5(password).toUpperCase();
        String password2 = user.getPasswd().toUpperCase();
        if (!password.equals(password2)) {
            //密码不正确
            addWrongLoginTime(ip);
            return Constant.ERROR_PWD;
        }
        // 查询权限相关,取消权限这一块
        if (false) {
            addWrongLoginTime(ip);
            return Constant.NO_PERMISSION;
        }
        //生成token存到redis,返回登录成功
        String token = MD5Util.trueMd5(Long.toString(new Date().getTime()));
        // 登录有效期为120分钟 两个小时
        boolean rs = RedisUtil.redisUtil.tokenToRedis(user.getId(), token, 120);
        if (rs) {
            // 登录成功需要清除登录次数记录
            clearWrongLoginTime(ip);
            return user.getId();
        } else {
            addWrongLoginTime(ip);
            return Constant.UNKNOWN_ERROR;
        }
    }
}
