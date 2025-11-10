package com.example.chalegesproject.security;

import com.example.chalegesproject.model.Users;
import com.example.chalegesproject.service.UsersRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    UsersRepository usersRepository;



    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //לאמת את המשתמש עם המשתמש שנמצא ב-DB
        Users user=usersRepository.findByUserName(username);
        if (user==null)
            throw new UsernameNotFoundException("user not found");
        //רשימה של הרשאות
        List<GrantedAuthority> grantedAuthorities=new ArrayList<>();
//        for(Role role:user.getRoles())
//        {
////            grantedAuthorities.add(new SimpleGrantedAuthority(role.getName().name()));
//        }
        grantedAuthorities.add(new SimpleGrantedAuthority("USER")); // אפשר לתת שם כלשהו
        return new CustomUserDetails(username,user.getPassword(),grantedAuthorities);//יוצר משתמש עבור האבטחה
    }
}

