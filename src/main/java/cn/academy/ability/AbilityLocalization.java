package cn.academy.ability;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.util.ACI18n;


/**
 * This class should be used only in client part.
 */
public enum AbilityLocalization {
    instance;
    
    public String levelDesc(int level) {
        return ACI18n.translateFormatted("ac.ability.level" + level);
    }
    
    public String machineType(DeveloperType type) {
        return local("type_" + type.toString().toLowerCase());
    }
    
    public String local(String key) {
        return ACI18n.translateFormatted("ac.skill_tree." + key);
    }
    
    public String local(String key, Object ...args) {
        return ACI18n.translateFormatted("ac.skill_tree." + key, args);
    }
    
}