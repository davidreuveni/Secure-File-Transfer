package com.davidr.secureft.services;

import org.springframework.stereotype.Service;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.repositories.UserRepo;

@Service
public class UserService {
    private UserRepo userRepo;
    public UserService(UserRepo userRepo){
        this.userRepo = userRepo;
    }
    
    public boolean addUserToDB(User a){
        if(userRepo.existsById(a.getUsername()))return false;
        userRepo.insert(a);
        return true;
    }  

    public boolean checkLogin(String un, String pw){
        User a = new User(un,pw);
        User b = userRepo.findById(a.getUsername()).orElse(null);
        boolean c = false;

        if(b==null)return c;
         if(a.getPassword().equals(b.getPassword()))c=true;
        return c;
    }
    
}
