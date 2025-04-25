CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `blog` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- 插入用户 Tom 和 Jerry
INSERT INTO `user` (`name`) VALUES ('Tom'), ('Jerry');
-- Tom 的博客（假设 Tom 的 user_id=1）
INSERT INTO `blog` (`user_id`, `title`, `content`) VALUES
(1, 'Tom''s First Blog', 'Hello, I''m Tom. This is my first blog post!'),
(1, 'Tom''s Travel Diary', 'Visited Paris last week. The Eiffel Tower was amazing!'),
(1, 'Tom''s Tech Tips', 'How to optimize MySQL queries for better performance.');
-- Jerry 的博客（假设 Jerry 的 user_id=2）
INSERT INTO `blog` (`user_id`, `title`, `content`) VALUES
(2, 'Jerry''s Cooking Blog', 'My secret recipe for the perfect cheesecake.'),
(2, 'Jerry on Gardening', '5 easy tips to grow tomatoes in small spaces.'),
(2, 'Jerry''s Book Review', 'Review of "The Alchemist" by Paulo Coelho.'),
(2, 'Jerry''s Fitness Journey', 'How I lost 10kg in 3 months with daily workouts.'),
(2, 'Jerry''s Music Playlist', 'My top 10 favorite jazz albums of all time.');