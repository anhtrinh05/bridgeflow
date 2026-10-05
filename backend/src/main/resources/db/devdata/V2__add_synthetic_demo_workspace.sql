-- Synthetic portfolio data only. No customer or NDA-protected content.
INSERT INTO projects (id, code, name, customer_name, status, created_at, updated_at)
VALUES (
    '10000000-0000-4000-8000-000000000001',
    'EC-RENEWAL',
    'EC Portal Renewal',
    '株式会社みらい',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO requirements (id, project_id, display_key, status, created_at, updated_at)
VALUES
    ('20000000-0000-4000-8000-000000000014', '10000000-0000-4000-8000-000000000001', 'REQ-014', 'REVIEWING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-4000-8000-000000000015', '10000000-0000-4000-8000-000000000001', 'REQ-015', 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-4000-8000-000000000016', '10000000-0000-4000-8000-000000000001', 'REQ-016', 'REVIEWING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-4000-8000-000000000017', '10000000-0000-4000-8000-000000000001', 'REQ-017', 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO requirement_revisions (
    id, requirement_id, revision_number, japanese_text, vietnamese_text,
    change_type, review_status, created_at, confirmed_by, confirmed_at
)
VALUES
    (
        '30000000-0000-4000-8000-000000000014', '20000000-0000-4000-8000-000000000014', 1,
        '承認済みの申請については、管理者のみ編集可能とする。ただし、申請者による取消の場合の動作については別途協議とする。',
        'Đối với đơn đã được phê duyệt, chỉ quản trị viên mới có quyền chỉnh sửa. Hành vi khi người nộp đơn hủy sẽ được trao đổi riêng.',
        'ADDED', 'REVIEWING', CURRENT_TIMESTAMP, NULL, NULL
    ),
    (
        '30000000-0000-4000-8000-000000000015', '20000000-0000-4000-8000-000000000015', 1,
        '管理者は、指定した期間の申請履歴をCSV形式で出力できるものとする。出力項目は申請番号、申請者、申請日時、現在のステータスとする。',
        'Quản trị viên có thể xuất lịch sử đăng ký trong khoảng thời gian chỉ định dưới dạng CSV, gồm mã đơn, người nộp, thời gian nộp và trạng thái hiện tại.',
        'ADDED', 'CONFIRMED', CURRENT_TIMESTAMP, '40000000-0000-4000-8000-000000000001', CURRENT_TIMESTAMP
    ),
    (
        '30000000-0000-4000-8000-000000000016', '20000000-0000-4000-8000-000000000016', 1,
        'ユーザーの権限に応じて、申請詳細画面に表示する項目を制御する。権限と表示項目の対応は権限マトリクスを参照すること。',
        'Kiểm soát các trường hiển thị trên màn hình chi tiết đơn theo quyền người dùng. Tham chiếu ma trận phân quyền để xác định quan hệ.',
        'ADDED', 'REVIEWING', CURRENT_TIMESTAMP, NULL, NULL
    ),
    (
        '30000000-0000-4000-8000-000000000017', '20000000-0000-4000-8000-000000000017', 1,
        '申請のステータスが承認または却下に変更された場合、申請者へ通知メールを送信する。同一申請に対する重複送信を防止すること。',
        'Gửi email cho người nộp khi trạng thái đơn chuyển thành đã duyệt hoặc bị từ chối. Hệ thống phải ngăn gửi trùng cho cùng một đơn.',
        'ADDED', 'CONFIRMED', CURRENT_TIMESTAMP, '40000000-0000-4000-8000-000000000001', CURRENT_TIMESTAMP
    );

UPDATE requirements
SET current_revision_id = '30000000-0000-4000-8000-000000000015'
WHERE id = '20000000-0000-4000-8000-000000000015';

UPDATE requirements
SET current_revision_id = '30000000-0000-4000-8000-000000000017'
WHERE id = '20000000-0000-4000-8000-000000000017';
