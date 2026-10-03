// Third-person controller: virtual joystick (left half), camera drag (right half), jump/attack buttons, keyboard on
// desktop. Offline, local only. SPDX-License-Identifier: GPL-3.0-or-later
using UnityEngine;

namespace MyVeloren
{
    [RequireComponent(typeof(CharacterController))]
    public sealed class PlayerController : MonoBehaviour
    {
        public Humanoid Body;
        public Camera Cam;
        public float Yaw, Pitch = 18f, Dist = 6f;
        public Vector2 AutoMove; // autopilot input
        CharacterController cc;
        float vy;
        int joyId = -1, lookId = -1;
        Vector2 joyStart, joyVec, lastLook;
        bool jumpReq;
        public float Travelled { get; private set; }
        public int Jumps { get; private set; }
        public int Attacks { get; private set; }

        void Awake() { cc = GetComponent<CharacterController>(); }

        Rect JumpRect => new Rect(Screen.width - Screen.height * 0.22f, Screen.height * 0.62f, Screen.height * 0.18f, Screen.height * 0.18f);
        Rect AttackRect => new Rect(Screen.width - Screen.height * 0.44f, Screen.height * 0.72f, Screen.height * 0.18f, Screen.height * 0.18f);

        static Vector2 GuiPos(Vector2 p) => new Vector2(p.x, Screen.height - p.y);

        public void Jump() { jumpReq = true; }
        public void Attack() { Body.Attack(); Attacks++; }

        void HandleTouches()
        {
            for (int i = 0; i < Input.touchCount; i++)
            {
                var t = Input.GetTouch(i);
                var g = GuiPos(t.position);
                if (t.phase == TouchPhase.Began)
                {
                    if (JumpRect.Contains(g)) { Jump(); continue; }
                    if (AttackRect.Contains(g)) { Attack(); continue; }
                    if (t.position.x < Screen.width * 0.45f && joyId < 0) { joyId = t.fingerId; joyStart = t.position; joyVec = Vector2.zero; }
                    else if (lookId < 0) { lookId = t.fingerId; lastLook = t.position; }
                }
                else if (t.phase == TouchPhase.Moved || t.phase == TouchPhase.Stationary)
                {
                    if (t.fingerId == joyId) joyVec = Vector2.ClampMagnitude((t.position - joyStart) / (Screen.height * 0.12f), 1f);
                    if (t.fingerId == lookId)
                    {
                        var d = t.position - lastLook; lastLook = t.position;
                        Yaw += d.x * 180f / Screen.width; Pitch = Mathf.Clamp(Pitch - d.y * 120f / Screen.height, -10, 70);
                    }
                }
                else
                {
                    if (t.fingerId == joyId) { joyId = -1; joyVec = Vector2.zero; }
                    if (t.fingerId == lookId) lookId = -1;
                }
            }
        }

        void Update()
        {
            HandleTouches();
            Vector2 mv = joyVec;
            mv += new Vector2(Input.GetAxisRaw("Horizontal"), Input.GetAxisRaw("Vertical"));
            mv += AutoMove;
            if (Input.GetKeyDown(KeyCode.Space)) Jump();
            if (Input.GetMouseButtonDown(0) && Input.touchCount == 0) Attack();
            if (Input.GetMouseButton(1)) { Yaw += Input.GetAxis("Mouse X") * 3; Pitch = Mathf.Clamp(Pitch - Input.GetAxis("Mouse Y") * 3, -10, 70); }
            mv = Vector2.ClampMagnitude(mv, 1f);

            var fwd = Quaternion.Euler(0, Yaw, 0);
            var dir = fwd * new Vector3(mv.x, 0, mv.y);
            const float speed = 6f;
            if (cc.isGrounded) { vy = -1f; if (jumpReq) { vy = 8f; Jumps++; } }
            jumpReq = false;
            vy -= 22f * Time.deltaTime;
            var before = transform.position;
            cc.Move((dir * speed + Vector3.up * vy) * Time.deltaTime);
            var moved = transform.position - before; moved.y = 0;
            Travelled += moved.magnitude;
            if (dir.sqrMagnitude > 0.01f)
                Body.transform.rotation = Quaternion.Slerp(Body.transform.rotation, Quaternion.LookRotation(dir), Time.deltaTime * 12f);
            Body.Speed01 = Mathf.MoveTowards(Body.Speed01, mv.magnitude, Time.deltaTime * 6f);
            if (transform.position.y < -20) { cc.enabled = false; transform.position = FindObjectOfType<World>().Center + Vector3.up * 2; cc.enabled = true; }
        }

        void LateUpdate()
        {
            var target = transform.position + Vector3.up * 1.6f;
            var rot = Quaternion.Euler(Pitch, Yaw, 0);
            var want = target - rot * Vector3.forward * Dist;
            if (Physics.Linecast(target, want, out var hit) && hit.collider.gameObject != gameObject)
                want = hit.point + (target - want).normalized * 0.3f;
            Cam.transform.position = want;
            Cam.transform.rotation = rot;
        }

        void OnGUI()
        {
            if (!Application.isMobilePlatform && Input.touchCount == 0 && !Boot.ForceTouchUI) return;
            var s = Screen.height;
            GUI.color = new Color(1, 1, 1, 0.35f);
            var r = new Rect(s * 0.08f, s * 0.58f, s * 0.34f, s * 0.34f);
            GUI.Box(r, "");
            if (joyId >= 0) GUI.Box(new Rect(GuiPos(joyStart).x - s * 0.05f + joyVec.x * s * 0.12f, GuiPos(joyStart).y - s * 0.05f - joyVec.y * s * 0.12f, s * 0.1f, s * 0.1f), "");
            GUI.color = new Color(1, 1, 1, 0.6f);
            var st = new GUIStyle(GUI.skin.box) { fontSize = (int)(s * 0.035f), alignment = TextAnchor.MiddleCenter };
            GUI.Box(JumpRect, "JUMP", st);
            GUI.Box(AttackRect, "ATK", st);
        }
    }
}
