package at.sfischer.constraints.model;

public enum TypeEnum implements Type {
    ANY, NUMBER, BOOLEAN, STRING, COMPLEXTYPE;


    @Override
    public boolean canAssignTo(Type target) {
        if(target == ANY){
            return true;
        }

        switch (this){
            case NUMBER -> {
                return target == NUMBER;
            }
            case BOOLEAN -> {
                return target == BOOLEAN;
            }
            case STRING -> {
                return target == STRING;
            }
            case COMPLEXTYPE -> {
                return target == COMPLEXTYPE;
            }
            case ANY -> {
                return true;
            }
        }

        return false;
    }
}
