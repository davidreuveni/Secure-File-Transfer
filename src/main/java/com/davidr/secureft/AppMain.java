package com.davidr.secureft;

import java.time.LocalTime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;

@Push
@SpringBootApplication
//@Theme("mytheme")
public class AppMain implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(AppMain.class, args);
        System.out.println(LocalTime.now()+"\n==>Test1Application running");
    }
}