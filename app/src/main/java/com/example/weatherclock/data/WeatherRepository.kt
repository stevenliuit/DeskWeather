package com.example.weatherclock.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WeatherRepository {

    data class CityGroup(val region: String, val emoji: String, val cities: List<WeatherLocation>)

    private val allLocations = listOf(
        // 🇨🇳 中国大陆 - 华北 (北京/天津/河北/山西/内蒙古)
        WeatherLocation("北京", "bei jing", "B", 39.9042, 116.4074, "Asia/Shanghai", "北京市", "CN"),
        WeatherLocation("天津", "tian jin", "T", 39.3434, 117.3616, "Asia/Shanghai", "天津市", "CN"),
        WeatherLocation("石家庄", "shi jia zhuang", "S", 38.0428, 114.5149, "Asia/Shanghai", "河北省", "CN"),
        WeatherLocation("唐山", "tang shan", "T", 39.6243, 118.1942, "Asia/Shanghai", "河北省", "CN"),
        WeatherLocation("保定", "bao ding", "B", 38.8738, 115.4646, "Asia/Shanghai", "河北省", "CN"),
        WeatherLocation("太原", "tai yuan", "T", 37.8706, 112.5489, "Asia/Shanghai", "山西省", "CN"),
        WeatherLocation("大同", "da tong", "D", 40.0766, 113.2953, "Asia/Shanghai", "山西省", "CN"),
        WeatherLocation("呼和浩特", "hu he hao te", "H", 40.8424, 111.7499, "Asia/Shanghai", "内蒙古", "CN"),
        WeatherLocation("包头", "bao tou", "B", 40.6561, 109.8403, "Asia/Shanghai", "内蒙古", "CN"),

        // 🇨🇳 华东北 - 上海/江苏/浙江/安徽/山东/福建/江西
        WeatherLocation("上海", "shang hai", "S", 31.2304, 121.4737, "Asia/Shanghai", "上海市", "CN"),
        WeatherLocation("南京", "nan jing", "N", 32.0603, 118.7969, "Asia/Shanghai", "江苏省", "CN"),
        WeatherLocation("苏州", "su zhou", "S", 31.2989, 120.5853, "Asia/Shanghai", "江苏省", "CN"),
        WeatherLocation("无锡", "wu xi", "W", 31.4912, 120.3119, "Asia/Shanghai", "江苏省", "CN"),
        WeatherLocation("常州", "chang zhou", "C", 31.7724, 119.9461, "Asia/Shanghai", "江苏省", "CN"),
        WeatherLocation("南通", "nan tong", "N", 31.9809, 120.8942, "Asia/Shanghai", "江苏省", "CN"),
        WeatherLocation("徐州", "xu zhou", "X", 34.2044, 117.2857, "Asia/Shanghai", "江苏省", "CN"),
        WeatherLocation("扬州", "yang zhou", "Y", 32.3935, 119.4126, "Asia/Shanghai", "江苏省", "CN"),
        WeatherLocation("镇江", "zhen jiang", "Z", 32.1888, 119.4256, "Asia/Shanghai", "江苏省", "CN"),
        WeatherLocation("盐城", "yan cheng", "Y", 33.3496, 120.1633, "Asia/Shanghai", "江苏省", "CN"),
        WeatherLocation("淮安", "huai an", "H", 33.5519, 119.0153, "Asia/Shanghai", "江苏省", "CN"),
        WeatherLocation("泰州", "tai zhou", "T", 32.4551, 119.9230, "Asia/Shanghai", "江苏省", "CN"),
        WeatherLocation("连云港", "lian yun gang", "L", 34.5966, 119.2216, "Asia/Shanghai", "江苏省", "CN"),
        WeatherLocation("宿迁", "su qian", "S", 33.9631, 118.2755, "Asia/Shanghai", "江苏省", "CN"),
        WeatherLocation("杭州", "hang zhou", "H", 30.2741, 120.1551, "Asia/Shanghai", "浙江省", "CN"),
        WeatherLocation("宁波", "ning bo", "N", 29.8683, 121.5440, "Asia/Shanghai", "浙江省", "CN"),
        WeatherLocation("温州", "wen zhou", "W", 28.0006, 120.6994, "Asia/Shanghai", "浙江省", "CN"),
        WeatherLocation("嘉兴", "jia xing", "J", 30.7522, 120.7550, "Asia/Shanghai", "浙江省", "CN"),
        WeatherLocation("湖州", "hu zhou", "H", 30.8678, 120.0930, "Asia/Shanghai", "浙江省", "CN"),
        WeatherLocation("绍兴", "shao xing", "S", 30.0301, 120.5801, "Asia/Shanghai", "浙江省", "CN"),
        WeatherLocation("金华", "jin hua", "J", 29.0789, 119.6479, "Asia/Shanghai", "浙江省", "CN"),
        WeatherLocation("衢州", "qu zhou", "Q", 28.9700, 118.8596, "Asia/Shanghai", "浙江省", "CN"),
        WeatherLocation("舟山", "zhou shan", "Z", 29.9852, 121.5584, "Asia/Shanghai", "浙江省", "CN"),
        WeatherLocation("台州", "tai zhou", "T", 28.6560, 121.4207, "Asia/Shanghai", "浙江省", "CN"),
        WeatherLocation("丽水", "li shui", "L", 28.4680, 119.9227, "Asia/Shanghai", "浙江省", "CN"),
        WeatherLocation("合肥", "he fei", "H", 31.8206, 117.2272, "Asia/Shanghai", "安徽省", "CN"),
        WeatherLocation("芜湖", "wu hu", "W", 31.3529, 118.4337, "Asia/Shanghai", "安徽省", "CN"),
        WeatherLocation("蚌埠", "beng bu", "B", 32.9169, 117.3888, "Asia/Shanghai", "安徽省", "CN"),
        WeatherLocation("淮南", "huai nan", "H", 32.6263, 117.0003, "Asia/Shanghai", "安徽省", "CN"),
        WeatherLocation("马鞍山", "ma an shan", "M", 31.6686, 118.5097, "Asia/Shanghai", "安徽省", "CN"),
        WeatherLocation("济南", "ji nan", "J", 36.6512, 117.1205, "Asia/Shanghai", "山东省", "CN"),
        WeatherLocation("青岛", "qing dao", "Q", 36.0671, 120.3826, "Asia/Shanghai", "山东省", "CN"),
        WeatherLocation("烟台", "yan tai", "Y", 37.4639, 121.4482, "Asia/Shanghai", "山东省", "CN"),
        WeatherLocation("威海", "wei hai", "W", 37.5132, 122.1425, "Asia/Shanghai", "山东省", "CN"),
        WeatherLocation("潍坊", "wei fang", "W", 36.7067, 119.1619, "Asia/Shanghai", "山东省", "CN"),
        WeatherLocation("淄博", "zi bo", "Z", 36.8132, 118.0550, "Asia/Shanghai", "山东省", "CN"),
        WeatherLocation("临沂", "lin yi", "L", 35.1041, 118.3566, "Asia/Shanghai", "山东省", "CN"),
        WeatherLocation("枣庄", "zao zhuang", "Z", 34.8107, 117.3237, "Asia/Shanghai", "山东省", "CN"),
        WeatherLocation("日照", "ri zhao", "R", 35.4164, 119.5269, "Asia/Shanghai", "山东省", "CN"),
        WeatherLocation("福州", "fu zhou", "F", 26.0753, 119.2965, "Asia/Shanghai", "福建省", "CN"),
        WeatherLocation("厦门", "xia men", "X", 24.4798, 118.0894, "Asia/Shanghai", "福建省", "CN"),
        WeatherLocation("泉州", "quan zhou", "Q", 24.8741, 118.6757, "Asia/Shanghai", "福建省", "CN"),
        WeatherLocation("漳州", "zhang zhou", "Z", 24.5122, 117.6472, "Asia/Shanghai", "福建省", "CN"),
        WeatherLocation("南昌", "nan chang", "N", 28.6829, 115.8579, "Asia/Shanghai", "江西省", "CN"),
        WeatherLocation("九江", "jiu jiang", "J", 29.7050, 116.0017, "Asia/Shanghai", "江西省", "CN"),
        WeatherLocation("赣州", "gan zhou", "G", 25.8452, 114.9333, "Asia/Shanghai", "江西省", "CN"),

        // 🇨🇳 华中南 - 河南/湖北/湖南/广东/广西/海南
        WeatherLocation("郑州", "zheng zhou", "Z", 34.7466, 113.6253, "Asia/Shanghai", "河南省", "CN"),
        WeatherLocation("洛阳", "luo yang", "L", 34.6182, 112.4540, "Asia/Shanghai", "河南省", "CN"),
        WeatherLocation("开封", "kai feng", "K", 34.7972, 114.3074, "Asia/Shanghai", "河南省", "CN"),
        WeatherLocation("武汉", "wu han", "W", 30.5928, 114.3055, "Asia/Shanghai", "湖北省", "CN"),
        WeatherLocation("宜昌", "yi chang", "Y", 30.6918, 111.2868, "Asia/Shanghai", "湖北省", "CN"),
        WeatherLocation("襄阳", "xiang yang", "X", 32.0091, 112.1226, "Asia/Shanghai", "湖北省", "CN"),
        WeatherLocation("长沙", "chang sha", "C", 28.2282, 112.9388, "Asia/Shanghai", "湖南省", "CN"),
        WeatherLocation("株洲", "zhu zhou", "Z", 27.8274, 113.1348, "Asia/Shanghai", "湖南省", "CN"),
        WeatherLocation("湘潭", "xiang tan", "X", 27.8294, 112.9441, "Asia/Shanghai", "湖南省", "CN"),
        WeatherLocation("衡阳", "heng yang", "H", 26.8935, 112.5719, "Asia/Shanghai", "湖南省", "CN"),
        WeatherLocation("岳阳", "yue yang", "Y", 29.3570, 113.1287, "Asia/Shanghai", "湖南省", "CN"),
        WeatherLocation("广州", "guang zhou", "G", 23.1291, 113.2644, "Asia/Shanghai", "广东省", "CN"),
        WeatherLocation("深圳", "shen zhen", "S", 22.5431, 114.0579, "Asia/Shanghai", "广东省", "CN"),
        WeatherLocation("东莞", "dong guan", "D", 23.0481, 113.7447, "Asia/Shanghai", "广东省", "CN"),
        WeatherLocation("佛山", "fo shan", "F", 23.0218, 113.1219, "Asia/Shanghai", "广东省", "CN"),
        WeatherLocation("珠海", "zhu hai", "Z", 22.2710, 113.5767, "Asia/Shanghai", "广东省", "CN"),
        WeatherLocation("中山", "zhong shan", "Z", 22.5176, 113.3926, "Asia/Shanghai", "广东省", "CN"),
        WeatherLocation("惠州", "hui zhou", "H", 23.1115, 114.4157, "Asia/Shanghai", "广东省", "CN"),
        WeatherLocation("江门", "jiang men", "J", 22.5784, 113.0814, "Asia/Shanghai", "广东省", "CN"),
        WeatherLocation("湛江", "zhan jiang", "Z", 21.1967, 110.4076, "Asia/Shanghai", "广东省", "CN"),
        WeatherLocation("汕头", "shan tou", "S", 23.3541, 116.6820, "Asia/Shanghai", "广东省", "CN"),
        WeatherLocation("南宁", "nan ning", "N", 22.8173, 108.3665, "Asia/Shanghai", "广西", "CN"),
        WeatherLocation("桂林", "gui lin", "G", 25.2744, 110.2900, "Asia/Shanghai", "广西", "CN"),
        WeatherLocation("柳州", "liu zhou", "L", 24.3266, 109.4286, "Asia/Shanghai", "广西", "CN"),
        WeatherLocation("北海", "bei hai", "B", 21.4734, 109.1193, "Asia/Shanghai", "广西", "CN"),
        WeatherLocation("海口", "hai kou", "H", 20.0444, 110.3497, "Asia/Shanghai", "海南省", "CN"),
        WeatherLocation("三亚", "san ya", "S", 18.2526, 109.5117, "Asia/Shanghai", "海南省", "CN"),

        // 🇨🇳 西南 - 重庆/四川/贵州/云南/西藏
        WeatherLocation("重庆", "chong qing", "C", 29.4316, 106.9123, "Asia/Shanghai", "重庆市", "CN"),
        WeatherLocation("成都", "cheng du", "C", 30.5728, 104.0668, "Asia/Shanghai", "四川省", "CN"),
        WeatherLocation("绵阳", "mian yang", "M", 31.4675, 104.6796, "Asia/Shanghai", "四川省", "CN"),
        WeatherLocation("宜宾", "yi bin", "Y", 28.7517, 104.6417, "Asia/Shanghai", "四川省", "CN"),
        WeatherLocation("南充", "nan chong", "N", 30.8375, 106.1107, "Asia/Shanghai", "四川省", "CN"),
        WeatherLocation("贵阳", "gui yang", "G", 26.6470, 106.6302, "Asia/Shanghai", "贵州省", "CN"),
        WeatherLocation("遵义", "zun yi", "Z", 27.7255, 106.9271, "Asia/Shanghai", "贵州省", "CN"),
        WeatherLocation("昆明", "kun ming", "K", 25.0406, 102.7129, "Asia/Shanghai", "云南省", "CN"),
        WeatherLocation("大理", "da li", "D", 25.6069, 100.2676, "Asia/Shanghai", "云南省", "CN"),
        WeatherLocation("丽江", "li jiang", "L", 26.8721, 100.2288, "Asia/Shanghai", "云南省", "CN"),
        WeatherLocation("拉萨", "la sa", "L", 29.6500, 91.1000, "Asia/Shanghai", "西藏", "CN"),

        // 🇨🇳 西北 - 陕西/甘肃/青海/宁夏/新疆
        WeatherLocation("西安", "xi an", "X", 34.3416, 108.9398, "Asia/Shanghai", "陕西省", "CN"),
        WeatherLocation("宝鸡", "bao ji", "B", 34.3616, 107.2372, "Asia/Shanghai", "陕西省", "CN"),
        WeatherLocation("咸阳", "xian yang", "X", 34.3295, 108.7092, "Asia/Shanghai", "陕西省", "CN"),
        WeatherLocation("延安", "yan an", "Y", 36.5853, 109.4897, "Asia/Shanghai", "陕西省", "CN"),
        WeatherLocation("兰州", "lan zhou", "L", 36.0611, 103.8343, "Asia/Shanghai", "甘肃省", "CN"),
        WeatherLocation("天水", "tian shui", "T", 34.5808, 105.7243, "Asia/Shanghai", "甘肃省", "CN"),
        WeatherLocation("西宁", "xi ning", "X", 36.6171, 101.7782, "Asia/Shanghai", "青海省", "CN"),
        WeatherLocation("银川", "yin chuan", "Y", 38.4876, 106.2309, "Asia/Shanghai", "宁夏", "CN"),
        WeatherLocation("乌鲁木齐", "wu lu mu qi", "W", 43.8256, 87.6168, "Asia/Shanghai", "新疆", "CN"),
        WeatherLocation("克拉玛依", "ke la ma yi", "K", 45.5798, 84.8892, "Asia/Shanghai", "新疆", "CN"),

        // 🇨🇳 东北 - 辽宁/吉林/黑龙江
        WeatherLocation("沈阳", "shen yang", "S", 41.8057, 123.4328, "Asia/Shanghai", "辽宁省", "CN"),
        WeatherLocation("大连", "da lian", "D", 38.9140, 121.6147, "Asia/Shanghai", "辽宁省", "CN"),
        WeatherLocation("鞍山", "an shan", "A", 41.1068, 122.9946, "Asia/Shanghai", "辽宁省", "CN"),
        WeatherLocation("抚顺", "fu shun", "F", 41.8807, 123.9574, "Asia/Shanghai", "辽宁省", "CN"),
        WeatherLocation("长春", "chang chun", "C", 43.8168, 125.3245, "Asia/Shanghai", "吉林省", "CN"),
        WeatherLocation("吉林", "ji lin", "J", 43.8378, 126.5494, "Asia/Shanghai", "吉林省", "CN"),
        WeatherLocation("哈尔滨", "ha er bin", "H", 45.8038, 126.5340, "Asia/Shanghai", "黑龙江省", "CN"),
        WeatherLocation("大庆", "da qing", "D", 46.5907, 125.1030, "Asia/Shanghai", "黑龙江省", "CN"),
        WeatherLocation("齐齐哈尔", "qi qi ha er", "Q", 47.3543, 123.9180, "Asia/Shanghai", "黑龙江省", "CN"),

        // 🇨🇳 港澳台
        WeatherLocation("香港", "xiang gang", "X", 22.3193, 114.1694, "Asia/Hong_Kong", "中国香港", "HK"),
        WeatherLocation("澳门", "ao men", "A", 22.1987, 113.5439, "Asia/Macau", "中国澳门", "MO"),
        WeatherLocation("台北", "tai bei", "T", 25.0330, 121.5654, "Asia/Taipei", "中国台湾", "TW"),
        WeatherLocation("高雄", "gao xiong", "G", 22.6273, 120.3014, "Asia/Taipei", "中国台湾", "TW"),
        WeatherLocation("新北", "xin bei", "X", 25.0120, 121.4657, "Asia/Taipei", "中国台湾", "TW"),

        // 🇯🇵 日本 (4)
        WeatherLocation("东京", "dong jing", "D", 35.6762, 139.6503, "Asia/Tokyo", "日本", "JP"),
        WeatherLocation("大阪", "da ban", "D", 34.6937, 135.5023, "Asia/Tokyo", "日本", "JP"),
        WeatherLocation("京都", "jing du", "J", 35.0116, 135.7681, "Asia/Tokyo", "日本", "JP"),
        WeatherLocation("札幌", "zha huan", "Z", 43.0618, 141.3545, "Asia/Tokyo", "日本", "JP"),

        // 🇰🇷 韩国 (1)
        WeatherLocation("首尔", "shou er", "S", 37.5665, 126.9780, "Asia/Seoul", "韩国", "KR"),

        // 🌏 东南亚 (8)
        WeatherLocation("新加坡", "xin jia po", "X", 1.3521, 103.8198, "Asia/Singapore", "新加坡", "SG"),
        WeatherLocation("曼谷", "man gu", "M", 13.7563, 100.5018, "Asia/Bangkok", "泰国", "TH"),
        WeatherLocation("清迈", "qing mai", "Q", 18.7883, 98.9853, "Asia/Bangkok", "泰国", "TH"),
        WeatherLocation("吉隆坡", "ji long po", "J", 3.1390, 101.6869, "Asia/Kuala_Lumpur", "马来西亚", "MY"),
        WeatherLocation("槟城", "bin cheng", "B", 5.4164, 100.3327, "Asia/Kuala_Lumpur", "马来西亚", "MY"),
        WeatherLocation("雅加达", "ya jia da", "Y", -6.2088, 106.8456, "Asia/Jakarta", "印度尼西亚", "ID"),
        WeatherLocation("巴厘岛", "ba li dao", "B", -8.3405, 115.0920, "Asia/Makassar", "印度尼西亚", "ID"),
        WeatherLocation("马尼拉", "ma ni la", "M", 14.5995, 120.9842, "Asia/Manila", "菲律宾", "PH"),
        WeatherLocation("胡志明市", "hu zhi ming shi", "H", 10.8231, 106.6297, "Asia/Ho_Chi_Minh", "越南", "VN"),

        // 🇮🇳 南亚 (4)
        WeatherLocation("新德里", "xin de li", "X", 28.6139, 77.2090, "Asia/Kolkata", "印度", "IN"),
        WeatherLocation("孟买", "meng mai", "M", 19.0760, 72.8777, "Asia/Kolkata", "印度", "IN"),
        WeatherLocation("班加罗尔", "ban jia luo er", "B", 12.9716, 77.5946, "Asia/Kolkata", "印度", "IN"),
        WeatherLocation("科伦坡", "ke lun po", "K", 6.9271, 79.8612, "Asia/Colombo", "斯里兰卡", "LK"),

        // 🇦🇪 中东 (5)
        WeatherLocation("迪拜", "di bai", "D", 25.2048, 55.2708, "Asia/Dubai", "阿联酋", "AE"),
        WeatherLocation("阿布扎比", "a bu zha bi", "A", 24.4539, 54.3773, "Asia/Dubai", "阿联酋", "AE"),
        WeatherLocation("利雅得", "li ya de", "L", 24.7136, 46.6753, "Asia/Riyadh", "沙特阿拉伯", "SA"),
        WeatherLocation("多哈", "duo ha", "D", 25.2854, 51.5310, "Asia/Qatar", "卡塔尔", "QA"),
        WeatherLocation("特拉维夫", "te la wei fu", "T", 32.0853, 34.7818, "Asia/Jerusalem", "以色列", "IL"),

        // 🇬🇧 欧洲 (15)
        WeatherLocation("伦敦", "lun dun", "L", 51.5074, -0.1278, "Europe/London", "英国", "GB"),
        WeatherLocation("曼彻斯特", "man che si te", "M", 53.4808, -2.2426, "Europe/London", "英国", "GB"),
        WeatherLocation("爱丁堡", "ai ding bao", "A", 55.9533, -3.1883, "Europe/London", "英国", "GB"),
        WeatherLocation("巴黎", "ba li", "B", 48.8566, 2.3522, "Europe/Paris", "法国", "FR"),
        WeatherLocation("马赛", "ma sai", "M", 43.2965, 5.3698, "Europe/Paris", "法国", "FR"),
        WeatherLocation("柏林", "bo lin", "B", 52.5200, 13.4050, "Europe/Berlin", "德国", "DE"),
        WeatherLocation("慕尼黑", "mu ni hei", "M", 48.1351, 11.5820, "Europe/Berlin", "德国", "DE"),
        WeatherLocation("法兰克福", "fa lan ke fu", "F", 50.1109, 8.6821, "Europe/Berlin", "德国", "DE"),
        WeatherLocation("罗马", "luo ma", "L", 41.9028, 12.4964, "Europe/Rome", "意大利", "IT"),
        WeatherLocation("米兰", "mi lan", "M", 45.4642, 9.1900, "Europe/Rome", "意大利", "IT"),
        WeatherLocation("威尼斯", "wei ni si", "W", 45.4408, 12.3155, "Europe/Rome", "意大利", "IT"),
        WeatherLocation("马德里", "ma de li", "M", 40.4168, -3.7038, "Europe/Madrid", "西班牙", "ES"),
        WeatherLocation("巴塞罗那", "ba sai luo na", "B", 41.3851, 2.1734, "Europe/Madrid", "西班牙", "ES"),
        WeatherLocation("阿姆斯特丹", "a mu si te dan", "A", 52.3676, 4.9041, "Europe/Amsterdam", "荷兰", "NL"),
        WeatherLocation("苏黎世", "su li shi", "S", 47.3769, 8.5417, "Europe/Zurich", "瑞士", "CH"),

        // 🇵🇱 北欧 + 🇷🇺 俄 (5)
        WeatherLocation("莫斯科", "mo si ke", "M", 55.7558, 37.6173, "Europe/Moscow", "俄罗斯", "RU"),
        WeatherLocation("圣彼得堡", "sheng bi de bao", "S", 59.9311, 30.3609, "Europe/Moscow", "俄罗斯", "RU"),
        WeatherLocation("哥本哈根", "ge ben ha gen", "G", 55.6761, 12.5683, "Europe/Copenhagen", "丹麦", "DK"),
        WeatherLocation("斯德哥尔摩", "si de ge er mo", "S", 59.3293, 18.0686, "Europe/Stockholm", "瑞典", "SE"),
        WeatherLocation("赫尔辛基", "he er xin ji", "H", 60.1699, 24.9384, "Europe/Helsinki", "芬兰", "FI"),

        // 🇺🇸 北美 (13)
        WeatherLocation("纽约", "niu yue", "N", 40.7128, -74.0060, "America/New_York", "美国", "US"),
        WeatherLocation("洛杉矶", "luo shan ji", "L", 34.0522, -118.2437, "America/Los_Angeles", "美国", "US"),
        WeatherLocation("旧金山", "jiu jin shan", "J", 37.7749, -122.4194, "America/Los_Angeles", "美国", "US"),
        WeatherLocation("芝加哥", "zhi jia ge", "Z", 41.8781, -87.6298, "America/Chicago", "美国", "US"),
        WeatherLocation("休斯顿", "xiu si dun", "X", 29.7604, -95.3698, "America/Chicago", "美国", "US"),
        WeatherLocation("迈阿密", "mai a mi", "M", 25.7617, -80.1918, "America/New_York", "美国", "US"),
        WeatherLocation("西雅图", "xi ya tu", "X", 47.6062, -122.3321, "America/Los_Angeles", "美国", "US"),
        WeatherLocation("波士顿", "bo shi dun", "B", 42.3601, -71.0589, "America/New_York", "美国", "US"),
        WeatherLocation("拉斯维加斯", "la si wei jia si", "L", 36.1699, -115.1398, "America/Los_Angeles", "美国", "US"),
        WeatherLocation("凤凰城", "feng huang cheng", "F", 33.4484, -112.0740, "America/Phoenix", "美国", "US"),
        WeatherLocation("华盛顿", "hua sheng dun", "H", 38.9072, -77.0369, "America/New_York", "美国", "US"),
        WeatherLocation("多伦多", "duo lun duo", "D", 43.6532, -79.3832, "America/Toronto", "加拿大", "CA"),
        WeatherLocation("温哥华", "wen ge hua", "W", 49.2827, -123.1207, "America/Vancouver", "加拿大", "CA"),
        WeatherLocation("蒙特利尔", "meng te li er", "M", 45.5017, -73.5673, "America/Toronto", "加拿大", "CA"),

        // 🇧🇷 拉美 (5)
        WeatherLocation("墨西哥城", "mo xi ge cheng", "M", 19.4326, -99.1332, "America/Mexico_City", "墨西哥", "MX"),
        WeatherLocation("坎昆", "kan kun", "K", 21.1619, -86.8515, "America/Cancun", "墨西哥", "MX"),
        WeatherLocation("圣保罗", "sheng bao luo", "S", -23.5505, -46.6333, "America/Sao_Paulo", "巴西", "BR"),
        WeatherLocation("里约热内卢", "li yue re nei lu", "L", -22.9068, -43.1729, "America/Sao_Paulo", "巴西", "BR"),
        WeatherLocation("布宜诺斯艾利斯", "bu yi nuo si ai li si", "B", -34.6037, -58.3816, "America/Argentina/Buenos_Aires", "阿根廷", "AR"),

        // 🇦🇺 大洋洲 (5)
        WeatherLocation("悉尼", "xi ni", "X", -33.8688, 151.2093, "Australia/Sydney", "澳大利亚", "AU"),
        WeatherLocation("墨尔本", "mo er ben", "M", -37.8136, 144.9631, "Australia/Melbourne", "澳大利亚", "AU"),
        WeatherLocation("布里斯班", "bu li si ban", "B", -27.4698, 153.0251, "Australia/Brisbane", "澳大利亚", "AU"),
        WeatherLocation("珀斯", "po si", "P", -31.9505, 115.8605, "Australia/Perth", "澳大利亚", "AU"),
        WeatherLocation("奥克兰", "ao ke lan", "A", -36.8485, 174.7633, "Pacific/Auckland", "新西兰", "NZ"),

        // 🇿🇦 非洲 (5)
        WeatherLocation("开罗", "kai luo", "K", 30.0444, 31.2357, "Africa/Cairo", "埃及", "EG"),
        WeatherLocation("约翰内斯堡", "yue han nei si bao", "Y", -26.2041, 28.0473, "Africa/Johannesburg", "南非", "ZA"),
        WeatherLocation("开普敦", "kai pu dun", "K", -33.9249, 18.4241, "Africa/Johannesburg", "南非", "ZA"),
        WeatherLocation("摩洛哥", "mo luo ge", "M", 31.7917, -7.0926, "Africa/Casablanca", "摩洛哥", "MA"),
        WeatherLocation("内罗毕", "nei luo bi", "N", -1.2921, 36.8219, "Africa/Nairobi", "肯尼亚", "KE")
    )

    fun getAllLocations(): List<WeatherLocation> = allLocations

    fun getLocationsByPinyinInitial(): Map<String, List<WeatherLocation>> {
        return allLocations.sortedBy { it.pinyinInitial }.groupBy { it.sortKey }
    }

    fun getPinyinInitials(): List<String> = allLocations.map { it.sortKey }.distinct().sorted()

    fun searchLocations(query: String): List<WeatherLocation> {
        if (query.isBlank()) return allLocations
        val q = query.trim().lowercase()
        // Fuzzy match: query fragments against name, pinyin, or country
        return allLocations
            .map { loc -> loc to when {
                // Exact/contains match (highest priority)
                loc.name.contains(query, ignoreCase = true) -> 0
                loc.country.contains(query, ignoreCase = true) -> 1
                loc.pinyinInitial.equals(q, ignoreCase = true) -> 2
                loc.pinyin == q -> 3
                // Fuzzy pinyin: each query char must appear in order in pinyin
                q.all { c -> loc.pinyin.indexOf(c, ignoreCase = true) >= 0 } -> 4
                // Partial pinyin word match
                q.split(" ").all { word -> loc.pinyin.contains(word) } -> 5
                else -> 99
            } }
            .filter { (_, priority) -> priority < 99 }
            .sortedBy { (_, priority) -> priority }
            .map { (loc, _) -> loc }
    }

    fun getCityGroups(): List<CityGroup> = listOf(
        CityGroup("中国", "🇨🇳", allLocations.filter { it.countryCode == "CN" || it.country == "中国香港" || it.country == "中国澳门" || it.country == "中国台湾" }),
        CityGroup("日韩", "🇯🇵", allLocations.filter { it.countryCode == "JP" || it.countryCode == "KR" }),
        CityGroup("东南亚", "🌏", allLocations.filter { it.countryCode in listOf("SG","TH","MY","ID","PH","VN") }),
        CityGroup("南亚", "🇮🇳", allLocations.filter { it.countryCode in listOf("IN","LK") }),
        CityGroup("中东", "🇦🇪", allLocations.filter { it.countryCode in listOf("AE","SA","QA","IL") }),
        CityGroup("欧洲", "🇬🇧", allLocations.filter { it.countryCode in listOf("GB","FR","DE","IT","ES","NL","CH","DK","SE","FI","RU") }),
        CityGroup("美洲", "🇺🇸", allLocations.filter { it.countryCode in listOf("US","CA","MX","BR","AR") }),
        CityGroup("大洋洲", "🏝️", allLocations.filter { it.countryCode in listOf("AU","NZ") }),
        CityGroup("非洲", "🇿🇦", allLocations.filter { it.countryCode in listOf("EG","ZA","MA","KE") })
    )

    fun getLocationByName(name: String): WeatherLocation? = allLocations.find { it.name == name }

    fun findNearestCity(lat: Double, lon: Double): WeatherLocation {
        return allLocations.minByOrNull { loc ->
            kotlin.math.sqrt(
                (loc.latitude - lat) * (loc.latitude - lat) +
                (loc.longitude - lon) * (loc.longitude - lon)
            )
        } ?: allLocations.first()
    }

    // ── 免费地理编码：按城市名搜索（Open-Meteo Geocoding API）──
    suspend fun searchCityByName(query: String, count: Int = 5): List<WeatherLocation> = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=$count&language=zh&format=json"
            val response = java.net.URL(url).readText()
            val json = parseJson(response)
            val results = json["results"] as? List<Any?> ?: emptyList()
            results.mapNotNull { r ->
                val map = r as? Map<String, Any?> ?: return@mapNotNull null
                val name = map["name"] as? String ?: return@mapNotNull null
                val lat = (map["latitude"] as? Number)?.toDouble() ?: return@mapNotNull null
                val lon = (map["longitude"] as? Number)?.toDouble() ?: return@mapNotNull null
                val country = map["country"] as? String ?: ""
                val countryCode = (map["country_code"] as? String ?: "").uppercase().take(2)
                val admin1 = map["admin1"] as? String ?: ""  // 省份/州
                val timezone = map["timezone"] as? String ?: "UTC"
                val displayName = if (admin1.isNotEmpty()) "$name, $admin1" else name
                WeatherLocation(
                    name = displayName,
                    pinyin = "",  // 动态搜索的城市没有拼音
                    pinyinInitial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    latitude = lat,
                    longitude = lon,
                    timezone = timezone,
                    country = country,
                    countryCode = countryCode
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ── 通过 GPS 坐标反向查询城市名（Open-Meteo Geocoding）──
    suspend fun reverseGeocode(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json&accept-language=zh"
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.setRequestProperty("User-Agent", "WeatherClock/1.0")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val response = connection.inputStream.bufferedReader().readText()
            val json = parseJson(response)
            val address = json["address"] as? Map<String, Any?> ?: emptyMap()
            val city = address["city"] as? String
                ?: address["town"] as? String
                ?: address["village"] as? String
                ?: address["municipality"] as? String
                ?: address["state"] as? String
                ?: address["country"] as? String
                ?: return@withContext null
            val country = address["country"] as? String ?: ""
            "$city, $country"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ── 天气数据 ──
    suspend fun getWeather(location: WeatherLocation): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val url = buildString {
                append("https://api.open-meteo.com/v1/forecast")
                append("?latitude=${location.latitude}")
                append("&longitude=${location.longitude}")
                append("&current=temperature_2m,relative_humidity_2m,apparent_temperature")
                append(",weather_code,wind_speed_10m,is_day")
                append("&daily=weather_code,temperature_2m_max,temperature_2m_min")
                append(",precipitation_probability_max,sunrise,sunset")
                append("&timezone=${location.timezone}")
                append("&forecast_days=7")
            }
            val response = java.net.URL(url).readText()
            val json = parseJson(response)
            val current = parseCurrentWeather(json)
            val daily = parseDailyForecast(json)
            Result.success(WeatherData(current, daily, location))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ── 空气质量数据（Open-Meteo 免费 API）──
    suspend fun getAirQuality(location: WeatherLocation): Result<AirQualityData> = withContext(Dispatchers.IO) {
        try {
            val url = buildString {
                append("https://air-quality-api.open-meteo.com/v1/air-quality")
                append("?latitude=${location.latitude}")
                append("&longitude=${location.longitude}")
                append("&current=us_aqi,pm10,pm2_5,ozone,no2,so2,co")
                append("&timezone=${location.timezone}")
            }
            val response = java.net.URL(url).readText()
            val json = parseJson(response)
            val current = json["current"] as? Map<String, Any?> ?: emptyMap()
            val aqi = (current["us_aqi"] as? Number)?.toInt() ?: 0
            val (level, color) = aqiLevel(aqi)
            Result.success(AirQualityData(
                usAqi = aqi,
                pm25 = (current["pm2_5"] as? Number)?.toDouble() ?: 0.0,
                pm10 = (current["pm10"] as? Number)?.toDouble() ?: 0.0,
                ozone = (current["ozone"] as? Number)?.toDouble() ?: 0.0,
                no2 = (current["no2"] as? Number)?.toDouble() ?: 0.0,
                so2 = (current["so2"] as? Number)?.toDouble() ?: 0.0,
                co = (current["co"] as? Number)?.toDouble() ?: 0.0,
                aqiLevel = level,
                aqiColor = color,
            ))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun aqiLevel(aqi: Int): Pair<String, Long> = when (aqi) {
        in 0..50   -> "优" to 0xFF4CAF50
        in 51..100 -> "良" to 0xFFFFEB3B
        in 101..150 -> "轻度" to 0xFFFF9800
        in 151..200 -> "中度" to 0xFFFF5722
        in 201..300 -> "重度" to 0xFFE91E63
        else        -> "严重" to 0xFF9C27B0
    }

    // ── JSON 解析 ──
    private fun parseJson(json: String): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        val s = json.trim()
        parseObject(s, 0, result) { _, map -> result.putAll(map) }
        return result
    }

    private fun parseObject(s: String, start: Int, result: MutableMap<String, Any?>, assign: (Int, Map<String, Any?>) -> Unit): Int {
        var i = start + 1
        val map = mutableMapOf<String, Any?>()
        while (i < s.length) {
            when (s[i]) {
                '"' -> {
                    val (key, _, nextI) = parseString(s, i)
                    i = nextI
                    while (i < s.length && s[i] == ' ') i++
                    if (s[i] == ':') i++
                    while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r')) i++
                    val (valueObj, newI) = parseValue(s, i)
                    i = newI
                    map[key] = valueObj
                }
                '}' -> { assign(i, map); return i + 1 }
                else -> i++
            }
        }
        return i
    }

    private data class StringResult(val str: String, val value: Any?, val nextIndex: Int)

    private fun parseString(s: String, start: Int): StringResult {
        var i = start + 1
        val sb = StringBuilder()
        while (i < s.length && s[i] != '"') {
            if (s[i] == '\\' && i + 1 < s.length) { i++; sb.append(s[i]) }
            else sb.append(s[i])
            i++
        }
        return StringResult(sb.toString(), null, i + 1)
    }

    private fun parseValue(s: String, start: Int): Pair<Any?, Int> {
        var i = start
        while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r' || s[i] == ',')) i++
        if (i >= s.length) return null to i
        return when {
            s[i] == '"' -> { val r = parseString(s, i); r.str to r.nextIndex }
            s[i] == '[' -> { val (list, nextI) = parseArray(s, i); list to nextI }
            s[i] == '{' -> { val map = mutableMapOf<String, Any?>()
                             i = parseObject(s, i, map) { idx, m -> i = idx; map.clear(); map.putAll(m) }
                             map to i }
            s[i] == 't' || s[i] == 'f' -> {
                val word = s.substring(i, minOf(i + 5, s.length))
                (word.startsWith("true")) to i + (if (word.startsWith("true")) 4 else 5)
            }
            s[i] == 'n' -> null to i + 4
            else -> {
                var j = i
                while (j < s.length && (s[j].isDigit() || s[j] == '.' || s[j] == '-' || s[j] == 'e' || s[j] == 'E' || s[j] == '+')) j++
                val numStr = s.substring(i, j)
                val value = if (numStr.contains('.')) numStr.toDoubleOrNull() else numStr.toIntOrNull()
                (value ?: numStr) to j
            }
        }
    }

    private fun parseArray(s: String, start: Int): Pair<List<Any?>, Int> {
        var i = start + 1
        val list = mutableListOf<Any?>()
        while (i < s.length) {
            while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r')) i++
            when {
                s[i] == ']' -> return list to i + 1
                s[i] == ',' -> { i++; continue }
                else -> { val (v, ni) = parseValue(s, i); list.add(v); i = ni }
            }
        }
        return list to i
    }

    private fun parseCurrentWeather(json: Map<String, Any?>): CurrentWeather {
        val current = json["current"] as? Map<String, Any?> ?: emptyMap()
        return CurrentWeather(
            temperature = (current["temperature_2m"] as? Number)?.toDouble() ?: 20.0,
            weatherCode = (current["weather_code"] as? Number)?.toInt() ?: 0,
            windSpeed = (current["wind_speed_10m"] as? Number)?.toDouble() ?: 0.0,
            humidity = (current["relative_humidity_2m"] as? Number)?.toInt() ?: 50,
            isDay = (current["is_day"] as? Number)?.toInt() == 1,
            feelsLike = (current["apparent_temperature"] as? Number)?.toDouble() ?: 20.0
        )
    }

    private fun parseDailyForecast(json: Map<String, Any?>): List<DailyForecast> {
        val daily = json["daily"] as? Map<String, Any?> ?: return emptyList()
        val times = (daily["time"] as? List<Any?>) ?: emptyList()
        val maxTemps = (daily["temperature_2m_max"] as? List<Any?>) ?: emptyList()
        val minTemps = (daily["temperature_2m_min"] as? List<Any?>) ?: emptyList()
        val codes = (daily["weather_code"] as? List<Any?>) ?: emptyList()
        val precips = (daily["precipitation_probability_max"] as? List<Any?>) ?: emptyList()
        val sunrises = (daily["sunrise"] as? List<Any?>) ?: emptyList()
        val sunsets = (daily["sunset"] as? List<Any?>) ?: emptyList()

        return times.mapIndexed { idx, _ ->
            DailyForecast(
                date = (times.getOrNull(idx) as? String) ?: "",
                tempMax = (maxTemps.getOrNull(idx) as? Number)?.toDouble() ?: 25.0,
                tempMin = (minTemps.getOrNull(idx) as? Number)?.toDouble() ?: 15.0,
                weatherCode = (codes.getOrNull(idx) as? Number)?.toInt() ?: 0,
                precipitationProbability = (precips.getOrNull(idx) as? Number)?.toInt() ?: 0,
                sunrise = (sunrises.getOrNull(idx) as? String) ?: "",
                sunset = (sunsets.getOrNull(idx) as? String) ?: ""
            )
        }
    }

    fun getWeatherIcon(weatherCode: Int, isDay: Boolean = true): String = when (weatherCode) {
        0 -> if (isDay) "☀️" else "🌙"
        1 -> if (isDay) "🌤️" else "🌤️"
        2 -> if (isDay) "⛅" else "☁️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57, 61, 63, 65 -> "🌧️"
        66, 67, 71, 73, 75, 77, 85, 86 -> "❄️"
        80, 81, 82 -> "🌦️"
        95, 96, 99 -> "⛈️"
        else -> if (isDay) "☀️" else "🌙"
    }

    fun getWeatherDescription(weatherCode: Int): String = when (weatherCode) {
        0 -> "晴朗"; 1 -> "晴间多云"; 2 -> "多云"; 3 -> "阴天"
        45 -> "雾"; 48 -> "雾凇"
        51 -> "小毛毛雨"; 53 -> "中毛毛雨"; 55 -> "大毛毛雨"
        56, 57 -> "冻毛毛雨"
        61 -> "小雨"; 63 -> "中雨"; 65 -> "大雨"
        66, 67 -> "冻雨"
        71 -> "小雪"; 73 -> "中雪"; 75 -> "大雪"; 77 -> "雪粒"
        80 -> "小阵雨"; 81 -> "中阵雨"; 82 -> "大阵雨"
        85, 86 -> "阵雪"
        95 -> "雷暴"; 96, 99 -> "雷暴冰雹"
        else -> "未知"
    }
}