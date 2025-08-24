package com.example.tiggle.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.*;
import java.util.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@Profile("local")
public class SshTunnelConfig {

    @Bean
    public static BeanFactoryPostProcessor dataSourceDependsOnSshTunnel() {
        return (ConfigurableListableBeanFactory bf) -> {
            for (String name : bf.getBeanNamesForType(DataSource.class, true, false)) {
                BeanDefinition bd = bf.getBeanDefinition(name);
                bd.setDependsOn(appendDependsOn(bd.getDependsOn(), "sshTunnelStarter"));
            }
            for (String name : bf.getBeanNamesForType(EntityManagerFactory.class, true, false)) {
                BeanDefinition bd = bf.getBeanDefinition(name);
                bd.setDependsOn(appendDependsOn(bd.getDependsOn(), "sshTunnelStarter"));
            }
            for (String name : bf.getBeanNamesForType(RedisConnectionFactory.class, true, false)) {
                BeanDefinition bd = bf.getBeanDefinition(name);
                bd.setDependsOn(appendDependsOn(bd.getDependsOn(), "sshTunnelStarter"));
            }
        };
    }

    private static String[] appendDependsOn(String[] current, String beanName) {
        if (current == null || current.length == 0) return new String[]{beanName};
        String[] next = Arrays.copyOf(current, current.length + 1);
        next[next.length - 1] = beanName;
        return next;
    }

    @Bean(name = "sshTunnelStarter")
    public Object sshTunnelStarterBean() {
        startSshTunnel();
        return new Object();
    }

    private void startSshTunnel() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            boolean isWindows = os.contains("windows");
            String knownHostsSink = isWindows ? "NUL" : "/dev/null";

            Path pem = resolvePemPath();
            System.out.println("[SSH] Using PEM: " + pem.toAbsolutePath());

            String sshExe = resolveSshExecutable(isWindows);
            System.out.println("[SSH] Using SSH: " + sshExe);

            if (isListening("127.0.0.1", 13306) && isListening("127.0.0.1", 16379)) {
                System.out.println("[SSH] Tunnel already listening");
                return;
            }

            List<String> cmd = new ArrayList<>(List.of(
                    sshExe,
                    "-i", pem.toAbsolutePath().toString(),
                    "-N",
                    "-L", "13306:127.0.0.1:3306",
                    "-L", "16379:127.0.0.1:6379",
                    "-o", "ExitOnForwardFailure=yes",
                    "-o", "ServerAliveInterval=60",
                    "-o", "BatchMode=yes",
                    "-o", "ConnectTimeout=10",
                    "-o", "StrictHostKeyChecking=no",
                    "-o", "UserKnownHostsFile=" + knownHostsSink,
                    "ubuntu@ec2-43-203-36-96.ap-northeast-2.compute.amazonaws.com"
            ));
            System.out.println("[SSH] Starting: " + String.join(" ", cmd));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(pem.getParent() != null ? pem.getParent().toFile() : new File("."));
            pb.redirectErrorStream(true);

            Process p = pb.start();

            new Thread(() -> {
                try (InputStream in = p.getInputStream()) { in.transferTo(System.out); }
                catch (Exception ignored) {}
            }, "ssh-log").start();

            long end = System.currentTimeMillis() + 30_000;
            while (System.currentTimeMillis() < end) {
                if (isListening("127.0.0.1", 13306) && isListening("127.0.0.1", 16379)) {
                    System.out.println("[SSH] Tunnel established");
                    return;
                }
                if (!p.isAlive()) {
                    throw new IllegalStateException("ssh exited early, code=" + p.exitValue());
                }
                Thread.sleep(300);
            }
            throw new IllegalStateException("Ports not open within timeout. See ssh-log above.");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start SSH tunnel: " + e.getMessage(), e);
        }
    }

    private Path resolvePemPath() {
        String override = System.getProperty("ssh.pem");
        if (override == null || override.isBlank()) override = System.getenv("SSH_PEM_PATH");
        if (override != null && !override.isBlank()) {
            Path p = toAbs(override); requireReadable(p); return p;
        }
        String[] candidates = { "taesan.pem", "./taesan.pem", "keys/taesan.pem" };
        for (String c : candidates) {
            Path p = toAbs(c);
            if (Files.isRegularFile(p) && Files.isReadable(p)) return p;
        }
        throw new IllegalStateException("PEM not found. Set -Dssh.pem or SSH_PEM_PATH, or place taesan.pem in working dir: " +
                Paths.get("").toAbsolutePath());
    }
    private Path toAbs(String in) {
        if (in.startsWith("~")) in = System.getProperty("user.home")+in.substring(1);
        Path p = Paths.get(in);
        if (!p.isAbsolute()) p = Paths.get(System.getProperty("user.dir")).resolve(p).normalize();
        return p;
    }
    private void requireReadable(Path p) {
        if (!Files.isRegularFile(p) || !Files.isReadable(p))
            throw new IllegalStateException("PEM not readable: " + p.toAbsolutePath());
    }
    private String resolveSshExecutable(boolean isWindows) {
        if (isWindows) {
            try {
                Process pr = new ProcessBuilder("where", "ssh").start();
                if (pr.waitFor() == 0) try (BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
                    String line = br.readLine();
                    if (line != null && !line.isBlank()) return line.trim();
                }
            } catch (Exception ignored) {}
            return "C:\\Windows\\System32\\OpenSSH\\ssh.exe";
        }
        return "ssh";
    }
    private boolean isListening(String host, int port) {
        try (Socket s = new Socket()) { s.connect(new InetSocketAddress(host, port), 200); return true; }
        catch (Exception e) { return false; }
    }
}
