package cn.academy.ability.context;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;


public interface IStateProvider {

    DelegateState getState();

}