package io.github.kloping.qqbot.http.data;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * 官方文档
 * <table><thead><tr><th><strong>属性</strong></th> <th><strong>类型</strong></th> <th><strong>说明</strong></th></tr></thead> <tbody><tr><td>id</td> <td>string</td> <td>消息唯一ID</td></tr> <tr><td>timestamp</td> <td>int</td> <td>发送时间</td></tr></tbody></table>
 *   实际则
 *   <hr>
 *   {"group_code":"","ret":1,"msg":""}
 * @author github.kloping
 */
@Data
public class V2Result {
    private String id;
    private Long timestamp;

    private String group_code;
    private Integer ret = 200;
    private String msg;

    public static String docMsg(String json) {
        V2Result result = JSON.parseObject(json, V2Result.class);
        return result.getMsg();
    }

    public static String docFiles(String json) {
        JSONObject data = JSON.parseObject(json);
        return data.getString("file_uuid");
    }
}
