package com.KDLST.Manager.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.sql.Date;
import com.KDLST.Manager.Model.Entity.CartItem.Cart;

import com.KDLST.Manager.Model.Entity.User.User;
import com.KDLST.Manager.Model.Service.CartItemService.CartService;
import com.KDLST.Manager.Model.Service.UserService.CustomerTypeServiceImplement;
import com.KDLST.Manager.Model.Service.UserService.UserServiceImplement;
import java.util.ArrayList;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping(value = "/user")
public class UserController {
    // Tạo user toàn cục
    private static User user;

    // Tạo đối tượng random
    Random random = new Random();

    // Tiêm phụ thuộc
    @Autowired
    private UserServiceImplement userServiceImplement = new UserServiceImplement();
    @Autowired
    private CustomerTypeServiceImplement customerTypeServiceImplement;
    @Autowired
    private CartService cartService;

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${liveUpload.path}")
    private String liveUploadPath;

    // Hàm check cookie, trả về form đăng nhập
    @GetMapping("/showLogin")
    public String showLogin(Model model, HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();
        User user = new User();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("userCookie".equals(cookie.getName())) {
                    String userStr = cookie.getValue();
                    user = userServiceImplement.login(userStr);
                    // Xử lý logic với username
                    HttpSession session = request.getSession(true);
                    session.setAttribute("user", user);
                    session.setAttribute("userRole", user.getRole());
                    model.addAttribute("user", user);
                    if (user.getRole().equals("ADMIN")) {
                        return "redirect:/admin/";
                    } else if (user.getRole().equals("EMPLOYEE")) {
                        return "redirect:/employee/";
                    }
                    return new IndexController().index(model);
                }
            }
        }
        model.addAttribute("user", user);
        return "User/login";
    }

    @GetMapping("/403")
    public String error() {
        return "User/403";
    }

    // Hàm check form và đăng nhập
    @PostMapping(value = "/login")
    public String toLogin(@ModelAttribute("user") User user1, Model model,
            @RequestParam(value = "agree", required = false) Boolean rememberme,
            HttpServletResponse response,
            HttpServletRequest request) {

        User user = new User();

        boolean flag = userServiceImplement.toLogin(user1);

        if (Boolean.TRUE.equals(rememberme)) {

            if (flag) {
                Cookie cookie = new Cookie("userCookie", user1.getEmail());
                user = userServiceImplement.login(user1.getEmail());
                cookie.setMaxAge(60 * 60);
                response.addCookie(cookie);
                HttpSession session = request.getSession(true);
                session.setAttribute("user", user);
                session.setAttribute("userRole", user.getRole());
                if (user.getRole().equals("ADMIN")) {
                    return "redirect:/admin/";
                } else if (user.getRole().equals("EMPLOYEE")) {
                    return "redirect:/employee/";
                }
                return new IndexController().index(model);
            } else {
                return showLogin(model, request);
            }
        } else if (!Boolean.TRUE.equals(rememberme)) {
            if (flag) {
                user = userServiceImplement.login(user1.getEmail());
                HttpSession session = request.getSession(true);
                session.setAttribute("user", user);
                session.setAttribute("userRole", user.getRole());
                if (user.getRole().equals("ADMIN")) {
                    return "redirect:/admin/";
                } else if (user.getRole().equals("EMPLOYEE")) {
                    return "redirect:/employee/";
                }
                return new IndexController().index(model);
            } else {
                return showLogin(model, request);
            }
        }
        return showLogin(model, request);
    }

    // Hàm trả về form đăng kí
    @GetMapping(value = { "/showRegister" })
    public String showRegister(Model model, String mess) {
        User user = new User();

        model.addAttribute("user", user);
        model.addAttribute("mess", mess);
        return "User/login";
    }

    // Hàm check form đăng kí
    @PostMapping(value = "/register")
    public String register(Model model, @ModelAttribute("user") User user1,
            @RequestParam(name = "passAgain") String pass, @RequestParam(name = "birth") String birth) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date utilDate;
        try {
            utilDate = dateFormat.parse(birth);
            Date sqlDate = new Date(utilDate.getTime());
            user1.setDob(sqlDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        user1.setAvatar("UserAvatarDefault.jpg");
        user1.setRole("CUSTOMER");
        user1.setCustomerType(customerTypeServiceImplement.getById(1));
        user1.setIdUser(0);
        user1.setStatus(true);
        if (pass.equals(user1.getPassword())) {
            if (userServiceImplement.getInvalidAttributes(user1).isEmpty()
                    || userServiceImplement.getInvalidAttributes(user1) == null) {
                user = user1;

                int randomNumber = random.nextInt(90000) + 10000;
                model.addAttribute("code", randomNumber);
                userServiceImplement.sendMail(user1.getEmail(), "Code Login for you",
                        randomNumber + "");
                return "User/SubmitCode";
            } else {
                ArrayList<String> errr = new ArrayList<>();
                errr = userServiceImplement.getInvalidAttributes(user1);
                String err = "";
                for (String loi : errr) {
                    err = err + " " + loi;
                }
                return showRegister(model, err);
            }
        } else {
            String mess = "passAgain";
            return showRegister(model, mess);
        }
    }

    // Hàm add 1 User
    @GetMapping("/toAdd")
    public String toAdd() {
        userServiceImplement.add(user);
        user = userServiceImplement.login(user.getEmail());
        Cart cart = new Cart(0, user);
        cartService.add(cart);
        user = null;
        return "redirect:/user/showLogin";

    }

    // Hàm trả về form đổi mk
    @GetMapping("/changePass")
    public String changePass() {
        return "User/ChangePass";
    }

    // Hàm generate code đổi mk
    @PostMapping("/toChangePass")
    public String toChangePass(@RequestParam(name = "email") String email, Model model) {
        int randomNumber = random.nextInt(90000) + 10000;
        ArrayList<User> users = userServiceImplement.getAll();
        for (User user : users) {
            if (user.getEmail().equals(email)) {
                model.addAttribute("code", randomNumber);
                userServiceImplement.sendMail(email, "Nhập code để thay đổi mật khẩu",
                        randomNumber + "");
                user = userServiceImplement.login(email);
                return "User/ToChangePass";
            }
        }
        return "User/ChangePass";
    }

    // Hàm đổi mk
    @PostMapping("/changePassword")
    public String changePass(@RequestParam(name = "password") String password) {
        User user1 = userServiceImplement.login(user.getEmail());
        user1.setPassword(password);
        userServiceImplement.update(user1);
        return "redirect:/user/showLogin";
    }

    // logout
    @GetMapping("/logout")
    public String index(Model model, HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("userCookie".equals(cookie.getName())) {
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }

        HttpSession session = request.getSession();
        session.removeAttribute("user");
        session.removeAttribute("userRole");
        return "redirect:/";
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);
        return "User/UserProfile";
    }

    @GetMapping("/showEdit")
    public String showEdit(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);
        return "User/Edit";
    }

    @PostMapping("/edit")
    public String edit(Model model, @ModelAttribute("user") User user1, HttpServletRequest request,
            @RequestParam("image") MultipartFile file) throws IOException {

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        // Tạo thư mục nếu chưa tồn tại
        Path uploadDirPath = Paths.get(uploadPath);
        Path liveUploadDirPath = Paths.get(liveUploadPath);

        if (!Files.exists(uploadDirPath)) {
            Files.createDirectories(uploadDirPath);
        }

        if (!Files.exists(liveUploadDirPath)) {
            Files.createDirectories(liveUploadDirPath);
        }

        if (!file.isEmpty()) {
            // Xóa tệp ảnh cũ nếu tồn tại
            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                Path oldFilePath = Paths.get(uploadPath, user.getAvatar());
                Path oldLiveFilePath = Paths.get(liveUploadPath, user.getAvatar());

                try {
                    Files.deleteIfExists(oldFilePath);
                    Files.deleteIfExists(oldLiveFilePath);
                } catch (IOException e) {
                    // Xử lý ngoại lệ nếu tệp không thể xóa
                    System.err.println("Could not delete file: " + e.getMessage());
                }
            }

            // Lưu tệp ảnh mới
            Path fileNameAndPath = Paths.get(uploadPath, file.getOriginalFilename());
            Path liveFileNameAndPath = Paths.get(liveUploadPath, file.getOriginalFilename());

            try {
                Files.write(fileNameAndPath, file.getBytes());
                Files.write(liveFileNameAndPath, file.getBytes());
                user1.setAvatar(file.getOriginalFilename());
            } catch (IOException e) {
                // Xử lý ngoại lệ nếu tệp không thể lưu
                System.err.println("Could not save file: " + e.getMessage());
            }
        } else {
            // Giữ lại ảnh hiện tại nếu không có tệp mới được tải lên
            user1.setAvatar(user.getAvatar());
            user.setAvatar(user.getAvatar());
        }

        // Sao chép các trường khác từ user trong session sang user1
        user1.setRole(user.getRole());
        user1.setCustomerType(user.getCustomerType());
        user1.setIdUser(user.getIdUser());
        user1.setStatus(user.getStatus());
        user1.setEmail(user.getEmail());
        user1.setPassword(user.getPassword());
        user1.setCardID(user.getCardID());
        userServiceImplement.update(user1);

        session.removeAttribute("user");
        session.setAttribute("user", user1);
        return "User/UserProfile";
    }

    @GetMapping("/showPassword")
    public String showPassword() {
        return "User/showPass";
    }

}
