package kr.hhplus.be.server.user;

import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    // 포인트 충전
    @PostMapping("/{userId}/points/charge")
    public ResponseEntity<Void> chargePoint(
            @PathVariable Long userId,
            @RequestParam int amount) {

        userService.chargePoint(userId, amount);
        return ResponseEntity.ok().build();
    }

    // 포인트 사용
    @PostMapping("/{userId}/points/use")
    public ResponseEntity<Void> usePoint(
            @PathVariable Long userId,
            @RequestParam int amount) {

        userService.usePoint(userId, amount);
        return ResponseEntity.ok().build();
    }

    // 포인트 잔액 조회
    @GetMapping("/{userId}/points")
    public ResponseEntity<PointBalanceResponse> getPointBalance(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));

        return ResponseEntity.ok(new PointBalanceResponse(user.getPointBalance()));
    }
}
