# V1.1 storyboard integration

The user supplied 25 original files across `SC5.zip`, `SC19.zip` and `sc15 1.zip`. All 24 images are integrated as optimised WebP scenes; the six-second MP4 was inspected and contributes a still at one second instead of a bundled video. The full-resolution originals remain outside the checkout in `/workspace/storyboard-reference`; only optimised runtime assets are committed.

Images preserve the source composition and characters: red school shirts, navy trousers, teachers in hijab, four coloured baskets, wooden fruit displays and warm supermarket lighting. Scene views fit the full image rather than stretching it. They decode one requested scene off the UI thread and release the previous view's image on detach.

| Original | Android asset | Where used |
| --- | --- | --- |
| Intro.png | story_intro | First-launch title |
| SC 0.jpg | story_supermarket | Intro / story reader: supermarket establishing shot |
| SC1.png | story_enter | Intro / story reader: entering supermarket |
| SC2.png | story_briefing | Teacher briefing, retry encouragement |
| Taklimat.jpg | story_instruction | Teacher instructions, How to Play |
| 3-2-1.jpg | story_countdown | Animated 3, 2, 1, MULA countdown |
| SC3.png | story_teamwork | Teamwork intro |
| SC4.png | story_ready | Ready-to-start intro |
| SC5.png | story_baskets | Basket selection |
| SC6.png | story_start | Story reader: hunt begins |
| SC7.png | story_fruit_hunt | Main menu / story reader; cropped ceiling lighting for gameplay |
| SC8.png | story_orange_team | Story reader: orange team |
| SC9.jpg | story_orange_basket | Story reader: orange basket |
| SC10.jpg | story_grape_team | Story reader: purple team |
| SC11.jpg | story_purple_basket | Story reader: purple basket |
| SC12.jpg | story_green_team | Story reader: green team |
| SC13.jpg | story_green_basket | Story reader: green basket |
| SC14.jpg | story_red_team | Story reader: red team |
| sc15 1.mp4 | story_red_basket | Reference clip; optimised still in story reader |
| SC16.png | story_montage | Story reader: basket montage |
| SC17.jpg | story_progress | Story reader / successful results |
| Tinggal 1 Minit.jpg | story_warning; teacher_warning crop | Teacher's 10-second warning during gameplay |
| SC18.jpg | story_finish | Ending: collected-fruit check |
| SC18.png | story_checkout | Ending: cashier checkout |
| SC19.png | story_celebration | Ending: group celebration |

## Optimisation

Runtime scenes live in `app/src/main/res/drawable-nodpi/`. Each full scene is bounded to 1024 × 576 pixels, preserving its aspect ratio, and encoded as WebP at quality 82. Full scenes plus the video still total about 2.1 MB. Additional small crops supply warm ceiling lighting (SC7) and the teacher warning. The gameplay aisle is a quiet, cached drawing inspired by the shelves and lighting, rather than a cluttered full storyboard image behind falling objects. Fruit and wrong-object sprites are small cached drawings with soft fruit shading.

`assets.json` records original filenames, source SHA-256 hashes, original/optimised dimensions and byte counts. It distinguishes the video reference from its derived still. No internet image/audio request or storage permission is required at runtime.
