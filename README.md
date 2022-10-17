We will use jjwt in this demo, we will use nimbus-jose in future demo as jjwt is deprecated.

based on this tutorial [Spring Security: Authentication Architecture Explained in Depth](https://youtu.be/ElY3rjtukig)

But I changed a lot of code to reflect spring security 5.7 updates.

## Environment

- Java 17
- Spring Boot 2.7.4
- jjwt (latest 2022 version)

## Checkpoint

- [x] Generate JWT Token in LoginController(POST /login)
- [x] Intercept(Use) JWT Bearer Token in JwtTokenFilter

Default.

![](./doc/images/default-impl.png)

To be ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

![](./doc/images/custom-impl.png)

## Checkpoint #1 Generate jwt token

We will use the following:

1. ~~built-in~~ Custom AuthenticationManager
2. built-in DaoAuthenticationProvider
3. custom UserDetailsService
4. Define our own JwtTokenFilter, add it before UsernamePasswordAuthenticationFilter

config/SecurityConfig.java 核心代码:

```java

@EnableWebSecurity
public class SecurityConfig {
    private final JwtTokenFilter jwtTokenFilter;

    public SecurityConfig(JwtTokenFilter jwtTokenFilter) {
        this.jwtTokenFilter = jwtTokenFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        var authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeRequests(auth -> auth
                        .antMatchers("/login").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

## Checkpoint #2 handle jwt bearer token in JwtTokenFilter

注意validate token或认证失败不必抛出异常， 让filterChain继续往下。。

utils/JwtTokenFilter.java 核心代码：

```java

@Component
public class JwtTokenFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain filterChain) {


        final String token = authorizationHeader.split(" ")[1].trim();
        if (!jwtUtil.validate(token)) {
            filterChain.doFilter(req, res);
            return;
        }

        // Authorization header exists, token is valid. So, we can authenticate.
        String username = jwtUtil.getUsername(token);
        UsernamePasswordAuthenticationToken upassToken = new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
        upassToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
        SecurityContextHolder.getContext().setAuthentication(upassToken);

        filterChain.doFilter(req, res);
    }
}

```

## Test

```shell
$ curl -H "content-type:application/json" \
-d '{"username":"cyper","password":"123"}' \
http://localhost:8080/login

$ curl -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjeXBlciIsImlzcyI6ImN5cGVyLnJ1biIsImlhdCI6MTY2NjAxNjY2NywiZXhwIjoxNjY2MDE2OTY3fQ.cMFisCSBbGXIvVXeMEkUcNuUt9MoSk5oRmun9aqjwos' \
http://localhost:8080/hello
```