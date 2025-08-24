package com.example.tiggle.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;

@Configuration
@Profile("local")
public class SshTunnelConfig {

    @Bean
    public ApplicationRunner sshTunnelRunner() {
        return args -> {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;
                
                if (os.contains("windows")) {
                    pb = new ProcessBuilder("ssh", "-i", "taesan.pem", "-N", 
                            "-L", "13306:127.0.0.1:3306", 
                            "-L", "16379:127.0.0.1:6379",
                            "-o", "ExitOnForwardFailure=yes",
                            "-o", "ServerAliveInterval=60",
                            "ubuntu@ec2-43-203-36-96.ap-northeast-2.compute.amazonaws.com");
                } else {
                    pb = new ProcessBuilder("ssh", "-i", "./taesan.pem", "-N",
                            "-L", "13306:127.0.0.1:3306",
                            "-L", "16379:127.0.0.1:6379", 
                            "-o", "ExitOnForwardFailure=yes",
                            "-o", "ServerAliveInterval=60",
                            "ubuntu@ec2-43-203-36-96.ap-northeast-2.compute.amazonaws.com");
                }
                
                pb.directory(new File("."));
                
                // 이미 실행중인 터널이 있는지 확인
                if (!isTunnelRunning()) {
                    System.out.println("Starting SSH tunnel...");
                    Process process = pb.start();
                    
                    // 터널이 설정될 때까지 잠시 대기
                    Thread.sleep(3000);
                    System.out.println("SSH tunnel established");
                } else {
                    System.out.println("SSH tunnel already running");
                }
                
            } catch (Exception e) {
                System.err.println("Failed to start SSH tunnel: " + e.getMessage());
            }
        };
    }
    
    private boolean isTunnelRunning() {
        try {
            ProcessBuilder pb = new ProcessBuilder("netstat", "-an");
            Process process = pb.start();
            
            String output = new String(process.getInputStream().readAllBytes());
            return output.contains(":13306") && output.contains(":16379");
        } catch (Exception e) {
            return false;
        }
    }
}