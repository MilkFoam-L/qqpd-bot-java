
import io.github.kloping.qqbot.Resource;
import io.github.kloping.qqbot.Starter;
import io.github.kloping.qqbot.api.Member;
import io.github.kloping.qqbot.api.Role;
import io.github.kloping.qqbot.api.Roles;
import io.github.kloping.qqbot.http.GuildBase;

import java.util.Arrays;

/**
 * @author github.kloping
 */
public class test_botRoles {
    public static void main(String[] args) {
        Starter starter = test_main.factory();
        starter.run();
        Roles roles = Resource.guildBase.getRoles(Resource.guildBase.getGuilds()[0].getId());
        System.out.println(roles);
    }
}
