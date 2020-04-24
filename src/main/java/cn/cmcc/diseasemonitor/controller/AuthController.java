package cn.cmcc.diseasemonitor.controller;

import cn.cmcc.diseasemonitor.service.UserService;
import cn.cmcc.diseasemonitor.util.*;
import cn.cmcc.diseasemonitor.util.constant.SmsType;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.OutputStream;

/**
 * 不知道怎么取名字 实验室人员相关接口： 登录 个人信息  修改个人信息等
 */
@RestController
@CrossOrigin
@RequestMapping("/lab")
public class AuthController {

    @Autowired
    UserService userService;

    @PostMapping("/token")
    @ApiOperation(value = "登录接口", notes = "传入用户名密码")
    public ResponseEntity login(@RequestParam String username
            , @RequestParam String password
            , @RequestParam(required = false) String verifyCode
            , HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        //校验密码
        Integer rs = userService.login(username, password, verifyCode, ip);
        return loginResult(rs);
    }

    @PostMapping("/phone/token")
    @ApiOperation(value = "手机号码登录接口", notes = "传入手机号和手机验证码")
    public ResponseEntity loginWithPhone(String phone, String phoneCode
            , HttpServletRequest request) {
        Integer rs = userService.loginWithPhone(phone, phoneCode, request);
        return loginResult(rs);
    }

    @GetMapping("/verifycode/need")
    @ApiOperation(value = "是否需要验证码", notes = "登录是否需要验证码")
    public ResponseEntity isNeedVerifyCode(HttpServletRequest request) {
        Boolean rs = userService.needVerifyCode(request);
        return ControllerUtil.getSuccessResultBySelf(rs);
    }

    @ApiOperation("生成验证码")
    @GetMapping("/verifycode")
    public void getCode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //利用图片工具生成图片
        //第一个参数是生成的验证码，第二个参数是生成的图片
        Object[] objs = VerifyCodeUtil.createImage();
        //将验证码存入验证码池
        VerifyCodeUtil.setVerifyCode(IpUtils.getIpAddr(request), objs[0].toString().toLowerCase());
        //将图片输出给浏览器
        BufferedImage image = (BufferedImage) objs[1];
        response.setContentType("image/png");
        OutputStream os = response.getOutputStream();
        ImageIO.write(image, "png", os);
    }

    /**
     * 传入手机号码，验证码，生成一个验证码存到redis
     * 前台通过手机接收到的验证码和密码进行修改密码
     */
    @GetMapping("/phonecode")
    @ApiOperation(value = "获取手机验证码")
    public ResponseEntity generateSMScode(HttpServletRequest request,
                                          @RequestParam(required = false) String verifyCode,
                                          String phone,
                                          @RequestHeader(required = false) String token) {
        return ControllerUtil.getDataResult(userService.generateSMScode(IpUtils.getIpAddr(request), verifyCode, phone, token));
    }

    /**
     * 传入手机号码，验证码，生成一个验证码存到redis
     * 前台通过手机接收到的验证码和密码进行修改密码
     */
    @GetMapping("/newphone/phonecode")
    @ApiOperation(value = "忘记密码获取验证码")
    public ResponseEntity generateSMScodeForNewPhone(HttpServletRequest request,
                                                     String phone, SmsType smsType) {
        return ControllerUtil.getDataResult(userService.generateSMScodeForNewPhone(IpUtils.getIpAddr(request), phone, smsType));
    }

    @PostMapping("/password")
    @ApiOperation(value = "修改密码,需要手机号, 验证码, 新的密码")
    public ResponseEntity changePwd(@RequestParam String phoneCode
            , @RequestParam String newPwd) {
        return ControllerUtil.getDataResult(userService.changePwd(phoneCode, newPwd));
    }

    @GetMapping("/token/expire")
    public ResponseEntity tokenExpire(@RequestHeader String token) {
        String tokenUserId = RedisUtil.getInstance().readDataFromRedis(token);
        return ControllerUtil.getTrueOrFalseResult(!(null == tokenUserId));
    }

    @GetMapping("/phonecode/expire")
    @ApiOperation(value = "判断手机验证码是否在redis中")
    public ResponseEntity phoneCodeExpire(String phoneCode) {
        String phone = RedisUtil.getInstance().readDataFromRedis(phoneCode);
        return ControllerUtil.getDataResult(phone);
    }

    /**
     * 登录结果处理
     *
     * @param rs
     * @return
     */
    private ResponseEntity loginResult(Integer rs) {
        if (null == rs) {
            rs = Constant.UNKNOWN_ERROR;
        }
        if (rs > 0) {
            //登录成功
            String token = RedisUtil.getInstance().readDataFromRedis("token" + rs.intValue());
            return ControllerUtil.getSuccessResultBySelf(token);
        } else if (rs == Constant.UNKNOWN_ERROR) {
            return ControllerUtil.getFalseResultMsgBySelf("未知错误");
        } else if (rs == Constant.NO_USER) {
            return ControllerUtil.getFalseResultMsgBySelf("没有找到该用户");
        } else if (rs == Constant.ERROR_PWD) {
            return ControllerUtil.getFalseResultMsgBySelf("密码错误");
        } else if (rs == Constant.NO_PERMISSION) {
            return ControllerUtil.getFalseResultMsgBySelf("权限错误");
        } else if (rs == Constant.VERIFYCODE_ERROR) {
            return ControllerUtil.getFalseResultMsgBySelf("验证码错误");
        } else if (rs == Constant.VERIFYCODE_NONE) {
            return ControllerUtil.getFalseResultMsgBySelf("未获取验证码，为空");
        } else {
            return ControllerUtil.getFalseResultMsgBySelf("权限错误2");
        }
    }
}
