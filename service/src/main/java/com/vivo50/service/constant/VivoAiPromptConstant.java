package com.vivo50.service.constant;

/**
 * @author Konjacer
 * @create 2025-06-10 22:01
 */
public class VivoAiPromptConstant {

    public static final String NOTE_TO_TAGS_PROMPT = "对下面这段笔记内容进行提炼，提炼出多个标签-类型entry，不要返回除了内容之外的任何数据：";

    public static final String NOTE_TO_TAGS_SYSTEM_PROMPT = "你返回的内容是多个标签-标签类型对，每个标签-类型entry之间用','分割，每个标签-类型entry中标签和类型之间用':'分割，注意分隔符都是英文字符。";

}
