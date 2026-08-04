package com.website.gis.core.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Tool command-line đơn giản để hash mật khẩu bằng BCryptPasswordEncoder
 * (giống hệt thuật toán mà backend Spring Security của project đang dùng).
 *
 * CÁCH DÙNG:
 * 1) Hash một mật khẩu mới:
 * java PasswordHashTool hash <mat_khau> [rounds]
 * Ví dụ:
 * java PasswordHashTool hash MatKhau123
 * java PasswordHashTool hash MatKhau123 12
 *
 * 2) Kiểm tra một mật khẩu có khớp với hash đã lưu trong DB không:
 * java PasswordHashTool check <mat_khau> <hash_trong_db>
 * Ví dụ:
 * java PasswordHashTool check 123456
 * "$2b$10$NZfABJasLYDBU9e.GCZ1TeTSC24MBTdnSg94bM8p1TKnR1QAOd3Wi"
 *
 * CÁCH BIÊN DỊCH & CHẠY:
 * Tool này cần thư viện spring-security-crypto trên classpath.
 *
 * - Nếu bạn đã có project backend (BE) của repo, chỉ cần copy file này vào
 * src/main/java/com/website/gis/ (đổi package cho khớp nếu muốn), rồi build
 * bằng Maven/Gradle như bình thường, sau đó chạy:
 * mvn compile exec:java -Dexec.mainClass="com.website.gis.PasswordHashTool"
 * -Dexec.args="hash MatKhau123"
 *
 * - Nếu muốn chạy độc lập (không cần cả project), tải jar
 * spring-security-crypto (và spring-security-core nếu cần) từ Maven Central:
 * https://mvnrepository.com/artifact/org.springframework.security/spring-security-crypto
 * rồi biên dịch/chạy với classpath trỏ tới các jar đó:
 * javac -cp spring-security-crypto-6.x.x.jar PasswordHashTool.java
 * java -cp .:spring-security-crypto-6.x.x.jar PasswordHashTool hash MatKhau123
 */
public class PasswordHashTool {

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }

        String mode = args[0];
        BCryptPasswordEncoder encoder;

        switch (mode) {
            case "hash": {
                String rawPassword = args[1];
                int rounds = args.length >= 3 ? Integer.parseInt(args[2]) : 10; // mặc định 10, giống project
                encoder = new BCryptPasswordEncoder(rounds);
                String hash = encoder.encode(rawPassword);

                System.out.println("Mật khẩu gốc : " + rawPassword);
                System.out.println("Số rounds    : " + rounds);
                System.out.println("Hash BCrypt  : " + hash);
                System.out.println();
                System.out.println("=> Copy chuỗi hash trên vào cột 'password' của bảng users để insert.");
                break;
            }
            case "check": {
                if (args.length < 3) {
                    printUsage();
                    return;
                }
                String rawPassword = args[1];
                String storedHash = args[2];
                encoder = new BCryptPasswordEncoder();
                boolean matches = encoder.matches(rawPassword, storedHash);

                System.out.println("Mật khẩu nhập : " + rawPassword);
                System.out.println("Hash trong DB : " + storedHash);
                System.out.println("Khớp không?   : " + (matches ? "CÓ - đăng nhập được" : "KHÔNG khớp"));
                break;
            }
            default:
                printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("Cách dùng:");
        System.out.println("  java PasswordHashTool hash <mat_khau> [rounds]");
        System.out.println("  java PasswordHashTool check <mat_khau> <hash_trong_db>");
    }
}