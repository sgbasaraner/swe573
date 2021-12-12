package io.github.sgbasaraner.funxchange.service;

import io.github.sgbasaraner.funxchange.entity.Follower;
import io.github.sgbasaraner.funxchange.entity.Interest;
import io.github.sgbasaraner.funxchange.entity.User;
import io.github.sgbasaraner.funxchange.model.AuthRequest;
import io.github.sgbasaraner.funxchange.model.AuthResponse;
import io.github.sgbasaraner.funxchange.model.NewUserDTO;
import io.github.sgbasaraner.funxchange.model.UserDTO;
import io.github.sgbasaraner.funxchange.repository.FollowerRepository;
import io.github.sgbasaraner.funxchange.repository.InterestRepository;
import io.github.sgbasaraner.funxchange.repository.UserRepository;
import io.github.sgbasaraner.funxchange.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.sasl.AuthenticationException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtTokenUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private FollowerRepository followerRepository;

    @Transactional
    public UserDTO signUp(NewUserDTO params) {
        if (!isUserNameValid(params.getUserName()))
            throw new IllegalArgumentException("Invalid username.");

        if (!isBioValid(params.getBio()))
            throw new IllegalArgumentException("Invalid bio.");

        if (!isPasswordValid(params.getPassword()))
            throw new IllegalArgumentException("Password should consist of more than 4 characters.");

        if (!areInterestsValid(params.getInterests()))
            throw new IllegalArgumentException("Invalid interest list.");

        interestRepository.saveAll(params.getInterests().stream().map(i -> {
            final Interest interest = new Interest();
            interest.setName(i);
            return interest;
        }).collect(Collectors.toSet()));

        final Set<Interest> interests = new HashSet<>(interestRepository.findByNameIn(params.getInterests()));

        final String passwordHash = passwordEncoder.encode(params.getPassword());
        final User userEntity = new User();
        userEntity.setBio(params.getBio());
        userEntity.setPasswordHash(passwordHash);
        userEntity.setUserName(params.getUserName());
        userEntity.setInterests(interests);

        try {
            final User createdUser = repository.save(userEntity);
            return mapUserToDTO(createdUser, createdUser);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Username already taken.");
        }
    }

    public AuthResponse createAuthenticationToken(AuthRequest authenticationRequest) throws AuthenticationException {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authenticationRequest.getUserName(), authenticationRequest.getPassword())
            );
        }
        catch (BadCredentialsException e) {
            throw new AuthenticationException();
        }


        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUserName());

        final String jwt = jwtTokenUtil.generateToken(userDetails);

        return new AuthResponse(jwt);
    }

    public UserDTO fetchUser(String id, Principal principal) {
        final User loggedInUser = repository.findUserByUserName(principal.getName()).get();
        final Optional<User> userOption = repository.findById(UUID.fromString(id));
        if (userOption.isEmpty())
            throw new IllegalArgumentException("User doesn't exist.");
        return mapUserToDTO(userOption.get(), loggedInUser);
    }

    private UserDTO mapUserToDTO(User user, User requestor) {

        Optional<Boolean> isFollowed;
        if (user.getId().equals(requestor.getId())) {
            isFollowed = Optional.empty();
        } else if (requestor.getFollows().stream().anyMatch(f -> f.getFollowee().getId().equals(user.getId()))) {
            isFollowed = Optional.of(true);
        } else {
            isFollowed = Optional.of(false);
        }

        return new UserDTO(
                user.getId().toString(),
                user.getUserName(),
                user.getBio(),
                followerRepository.findByFollowee(user).size(),
                user.getFollows().size(),
                user.getInterests().stream().map(Interest::getName).collect(Collectors.toUnmodifiableList()),
                isFollowed
        );
    }

    private Pageable makePageable(int offset, int limit, Sort sort) {
        if (offset < 0 || limit <= 0)
            throw new IllegalArgumentException("Invalid limit or offset");
        int currentPage = offset / limit;
        return PageRequest.of(currentPage, limit, sort);
    }

    public List<UserDTO> fetchFollowed(String id, int offset, int limit, Principal principal) {
        final User requestor = repository.findUserByUserName(principal.getName()).get();
        final Optional<User> targetUserOption = repository.findById(UUID.fromString(id));
        if (targetUserOption.isEmpty())
            throw new IllegalArgumentException("User doesn't exist.");

        final Pageable pageRequest = makePageable(offset, limit, Sort.by("created").descending());

        return followerRepository
                .findByFollower(targetUserOption.get(), pageRequest)
                .stream()
                .map(f -> mapUserToDTO(f.getFollowee(), requestor))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<UserDTO> fetchFollowers(String id, int offset, int limit, Principal principal) {
        final User requestor = repository.findUserByUserName(principal.getName()).get();
        final Optional<User> targetUserOption = repository.findById(UUID.fromString(id));
        if (targetUserOption.isEmpty())
            throw new IllegalArgumentException("User doesn't exist.");

        final Pageable pageRequest = makePageable(offset, limit, Sort.by("created").descending());

        return followerRepository
                .findByFollowee(targetUserOption.get(), pageRequest)
                .stream()
                .map(f -> mapUserToDTO(f.getFollower(), requestor))
                .collect(Collectors.toUnmodifiableList());
    }

    public String followUser(String userId, Principal principal) {
        final User loggedInUser = repository.findUserByUserName(principal.getName()).get();
        final Optional<User> followeeUserOption = repository.findById(UUID.fromString(userId));
        if (followeeUserOption.isEmpty())
            throw new IllegalArgumentException("User doesn't exist.");

        final User followeeUser = followeeUserOption.get();

        final Follower f = new Follower();
        f.setFollower(loggedInUser);
        f.setFollowee(followeeUser);
        followerRepository.save(f);
        return followeeUser.getId().toString();
    }

    public String unfollowUser(String userId, Principal principal) {
        final User loggedInUser = repository.findUserByUserName(principal.getName()).get();
        final Optional<User> followeeUserOption = repository.findById(UUID.fromString(userId));
        if (followeeUserOption.isEmpty())
            throw new IllegalArgumentException("User doesn't exist.");

        final User followeeUser = followeeUserOption.get();

        Optional<Follower> f = followerRepository.findByFolloweeAndFollower(followeeUser, loggedInUser);
        if (f.isEmpty()) {
            throw new IllegalArgumentException("Follow relation doesn't exist.");
        }
        followerRepository.delete(f.get());
        return f.get().getFollowee().getId().toString();
    }

    public UserDTO fetchUserByUserName(String userName, Principal principal) {
        final User loggedInUser = repository.findUserByUserName(principal.getName()).get();
        final Optional<User> userOption = repository.findUserByUserName(userName);
        if (userOption.isEmpty())
            throw new IllegalArgumentException("User doesn't exist.");
        return mapUserToDTO(userOption.get(), loggedInUser);
    }

    private static final List<String> allowedInterests = List
            .of("Golf", "Yoga", "Painting", "Graphic Design", "Computers", "Makeup", "Cooking", "Gaming");

    private boolean areInterestsValid(List<String> interests) {
        final List<String> distinct = interests.stream().distinct().collect(Collectors.toUnmodifiableList());
        if (distinct.size() != interests.size()) return false;
        return allowedInterests.containsAll(distinct);
    }

    private boolean isUserNameValid(String userName) {
        return !(userName == null || userName.isBlank() || userName.length() < 3);
    }

    private boolean isBioValid(String bio) {
        if (bio == null) return false;
        return bio.length() < 1024;
    }

    private boolean isPasswordValid(String password) {
        if (password == null) return false;
        return password.length() > 4 && password.length() < 1024;
    }
}
