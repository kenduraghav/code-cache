package com.developer.tool;

import org.mindrot.jbcrypt.BCrypt;

import io.javalin.http.Context;

public class AuthController {

	private final UserDAO userDAO;

	public AuthController(UserDAO userDAO) {
		this.userDAO = userDAO;
	}

	public void showLogin(Context ctx) {
		ctx.result(
				"""
						    <!DOCTYPE html>
						    <html>
						    <head><title>Login - CodeCache</title></head>
						    <body style='font-family: Arial; max-width: 400px; margin: 100px auto;'>
						        <h1>Login</h1>
						        <form method='post' action='/login'>
						            <input name='username' placeholder='Username' required style='display:block;margin:10px 0;padding:8px;width:100%%'><br>
						            <input type='password' name='password' placeholder='Password' required style='display:block;margin:10px 0;padding:8px;width:100%%'><br>
						            <button type='submit' style='padding:10px 20px;'>Login</button>
						            <a href='/register' style='margin-left:10px;'>Register</a>
						        </form>
						    </body>
						    </html>
						""");
	}

	public void registerUserForm(Context ctx) {
		ctx.result(
				"""
		           <!DOCTYPE html>
		           <html>
		           <head><title>Register - CodeCache</title></head>
		           <body style='font-family: Arial; max-width: 400px; margin: 100px auto;'>
		               <h1>Register</h1>
		               <form method='post' action='/register'>
		                   <input name='username' placeholder='Username' required style='display:block;margin:10px 0;padding:8px;width:100%%'><br>
		                   <input type='password' name='password' placeholder='Password' required style='display:block;margin:10px 0;padding:8px;width:100%%'><br>
		                   <button type='submit' style='padding:10px 20px;'>Register</button>
		                   <a href='/login' style='margin-left:10px;'>Login</a>
		               </form>
		           </body>
		           </html>
						""");
	}

	public void login(Context ctx) {
		// Login handler
		String username = ctx.formParam("username");
		String password = ctx.formParam("password");

		User user = userDAO.findByUsername(username);

		if (user == null) {
			return;
		}

		boolean isLoggedIn = BCrypt.checkpw(password, user.password());

		if (isLoggedIn) {
			ctx.sessionAttribute("userId", user.id());
			ctx.redirect("/");
		} else {
			ctx.result("Invalid credentials. <a href='/login'>Try again</a>");
		}
	}
	
	
	public void registerUser(Context ctx) {
		 String username = ctx.formParam("username");
         String password = ctx.formParam("password");
         
         try {
             userDAO.create(username, password);
             ctx.redirect("/login");
         } catch (Exception e) {
             ctx.result("Username taken. <a href='/register'>Try again</a>");
         }
	}
}
