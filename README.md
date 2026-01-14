# someone — Book Library / Smart Organizer

![Stars](https://img.shields.io/github/stars/kuum-oss/someone?style=flat-square)
![Forks](https://img.shields.io/github/forks/kuum-oss/someone?style=flat-square)
![Issues](https://img.shields.io/github/issues/kuum-oss/someone?style=flat-square)
![License](https://img.shields.io/github/license/kuum-oss/someone?style=flat-square)
![Top language](https://img.shields.io/github/languages/top/kuum-oss/someone?style=flat-square)
![Release](https://img.shields.io/github/v/release/kuum-oss/someone?style=flat-square)

Коротко
-------
Desktop-приложение на Java (Swing + FlatLaf), которое сканирует файлы электронных книг, извлекает метаданные (локально и через Google Books), показывает их в дереве по языку/жанру/серии и помогает организовать коллекцию в папки.

Основные возможности
--------------------
- Drag & drop: перетащите папку/файлы с книгами в окно.
- Автоматическое извлечение метаданных (Apache Tika): title, author, language, возможная серия, формат.
- Подкачка жанра и обложки из Google Books (при необходимости).
- Организация файлов в структуру: collection/<Language>/<Genre>/<Series>/<Title.ext>
- Поддерживаемые форматы: .pdf, .epub, .fb2, .mobi
- Простая локализация (en/ru) и переключение тем (light/dark/system) через меню.
- Небольшой кэш и отображение иконок жанров (GenreImageService).

Требования
----------
- Java 21 (в pom.xml указан target/source = 21)
- Maven (для сборки)
- Интернет (опционально) — для подтягивания жанра/обложки и загрузки GIF-иконок жанров

Сборка и запуск
---------------
Собрать проект:
```bash
mvn -DskipTests package
```

После успешной сборки будет jar с зависимостями (shade) в target. Запуск:
```bash
java -jar target/someone-1.0-SNAPSHOT.jar
```
или (если jar назван иначе — посмотрите содержимое target/)

Запуск тестов:
```bash
mvn test
```

Как пользоваться
----------------
1. Запустите приложение.
2. Перетащите в окно одну или несколько книг / папку с книгами.
3. Приложение просканирует файлы, извлечёт метаданные и отобразит дерево:
   - Язык → Жанр → Серия → Книга
4. Нажмите "Organize into folders" и выберите папку назначения. Файлы будут скопированы в структуру:
   collection/<Language>/<Genre>/<Series or omitted if no series>/<OriginalFilename>

Примеры структуры:
- collection/Русский/Фантастика/Игра/BookTitle.epub
- collection/English/Fantasy/No Series/SomeBook.pdf

Особенности реализации (коротко)
-------------------------------
- src/main/java/org/example/model/Book.java — модель книги (title, author, series, genre, language, path, format, cover).
- MetadataService — извлечение метаданных локально через Apache Tika; попытки извлечь обложку из EPUB/FB2; угадывание серии по ��мени файла; fallback-значения.
- ExternalMetadataService — поиск жанра и обложки через Google Books API (использует HttpClient + Jackson).
- FileService — логика копирования/создания директорий и локализации названий (getLocalizedLanguage/getLocalizedGenre).
- BookOrganizer — группировка списка книг для построения дерева (language → genre → series).
- GenreImageService — подгрузка иконок/гифок для жанров (кеширование).
- UI — BookLibraryGui (Swing, FlatLaf): drag&drop, дерево, прогресс, смена языка/темы.

Конфигурация и заметки
----------------------
- Google Books API: в коде используется публичный endpoint без API-ключа. Для надёжной работы в больших объёмах стоит добавить ключ и лимиты запросов.
- GenreImageService содержит набор внешних ссылок на GIF; в некоторых средах (CI, headless) загрузка иконок может не работать.
- Файлы копируются (Files.copy) с опцией REPLACE_EXISTING. Измените при необходимости на перемещение.
- Локализация: ресурсы в src/main/resources/messages_en.properties и messages_ru.properties. Русская локализация частично содержит неправильную кодировку/символы — проверьте и исправьте при необходимости.
- В UI используются FlatLaf темы: FlatMacLightLaf / FlatMacDarkLaf (подходят не только для macOS).

Ограничения и возможные улучшения
---------------------------------
- Улучшить обработку разных форматов (mobi, pdf с обложкой).
- Добавить конфигурацию для Google Books API key и rate limiting.
- Добавить прогресс-бар общей операции (оценка времени/осталось).
- Обрабатывать дубликаты по хэшу/метаданным.
- Поддержка дополнительных языков и корректная русская локализация.
- Переход от копирования к безопасному перемещению с опцией "dry-run".

Тесты
-----
В каталоге src/test/java находятся базовые тесты:
- ExternalMetadataServiceTest — простая проверка получения жанра для известной книги.
- GenreImageServiceTest — проверка получения иконки (может падать в headless/без-инета).


Контрибуция
----------
PR и issues приветствуются. Рекомендации:
- Описывайте изменения в title/description PR.
- Для новых фич — создайте issue сначала.
- Запускайте mvn test перед отправкой PR.


--------
Автор: kuum-oss  
Если нужна помощь с локализацией, интеграцией API-ключа Google Books или адаптацией под другие рабочие процессы — откройте issue или напишите PR.

Спасибо!
