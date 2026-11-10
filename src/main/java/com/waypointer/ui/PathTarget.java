package com.waypointer.ui;

// Something on screen that stands for one tile and can show that it is being pathed to. Lets
// the panel retint rows and tiles in place when the path target changes.
interface PathTarget
{
    int getPackedWorldPoint();

    void setActive(boolean active);
}
