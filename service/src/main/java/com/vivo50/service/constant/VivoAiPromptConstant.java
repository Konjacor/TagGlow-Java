package com.vivo50.service.constant;

/**
 * @author Konjacer
 * @create 2025-06-10 22:01
 */
public class VivoAiPromptConstant {

    public static final String NOTE_TO_TAGS_PROMPT = "对下面这段笔记内容进行提炼，提炼出多个标签-类型entry，不要返回除了内容之外的任何数据：";

    public static final String NOTE_TO_TAGS_SYSTEM_PROMPT = "你返回的内容是多个标签-标签类型对，每个标签-类型entry之间用','分割，每个标签-类型entry中标签和类型之间用':'分割，注意分隔符都是英文字符。";


    // 新增用于生成旅游攻略的提示
    public static final String NOTE_TO_TRAVEL_GUIDE_PROMPT = "以下是几篇笔记内容，请根据内容生成一篇有趣且详细的旅游攻略：";
    public static final String NOTE_TO_TRAVEL_GUIDE_SYSTEM_PROMPT = "请返回一篇几百字的旅游攻略，内容应包括地点、天气、时间和推荐活动等信息，不要返回任何多余的内容。";

    // 用于对笔记内容进行回复
    public static final String NOTE_TO_REPLY_PROMPT = "请根据用户提供的笔记内容进行回复，可以在有些情况下选择性地适当结合时间、地点、天气信息，注意内容友好、积极，不要返回任何多余内容。";
}
