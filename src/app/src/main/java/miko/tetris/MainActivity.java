package miko.tetris;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
{
    Bitmap bitmap;
    Canvas canvas;
    ImageView imageView;
    Random random;
    Timer timer;
    long exitTime = 0;

    TextView textView;
    TextView textView1;
    TextView textView2;
    TextView textView3;

    final TimerTask timerTask = new TimerTask()
    {
        @Override
        public void run()
        {
            if (!run || gameOver)
                return;
            timerTick.sendEmptyMessage(0);
        }
    };

    // timer 触发动作 不直接写在 run() 里以保证线程安全
    final Handler timerTick = new Handler()
    {
        public void handleMessage(Message msg)
        {
            moveDown();
            drawTetris();
            super.handleMessage(msg);
        }
    };

    // 设置游戏状态
    final Handler setGameState = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case stateGAMEOVER:
                    gameOver = true;
                    run = false;
                    textView.setText(R.string.game_over);
                    break;
                case stateGAMERUN:
                    if (!gameOver)
                    {
                        run = true;
                        reseted = false;
                        textView.setText(R.string.state_run);
                        Toast.makeText(MainActivity.this, R.string.game_started, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case stateGAMEPAUSE:
                    run = false;
                    textView.setText(R.string.state_pause);
                    Toast.makeText(MainActivity.this, R.string.game_paused, Toast.LENGTH_SHORT).show();
                    break;
                case stateGAMERESET:
                    reset();
                    gameOver = false;
                    run = false;
                    reseted = true;
                    textView.setText(getString(R.string.state_stop));
                    Toast.makeText(MainActivity.this, R.string.game_reseted, Toast.LENGTH_SHORT).show();
                    break;
            }
            drawTetris();
            super.handleMessage(msg);
        }
    };

    // Java 枚举转 int 有点蛋疼 省事写法
    final int stateGAMEOVER = 0;
    final int stateGAMERUN = 1;
    final int stateGAMEPAUSE = 2;
    final int stateGAMERESET = 3;

    // 预定义的几种颜色
    final int color[] =
            {
                    Color.argb(0, 255, 255, 255),
                    Color.argb(255, 255, 160, 122),
                    Color.argb(255, 128, 0, 128),
                    Color.argb(255, 173, 216, 230),
                    Color.argb(255, 205, 133, 63),
                    Color.argb(255, 143, 188, 143),
                    Color.argb(255, 255, 105, 180),
                    Color.argb(255, 255, 215, 0),
                    Color.argb(255, 128, 128, 128),
            };

    // 使用数组定义方块的形状 顺序与 cubeType 相同
    final byte[][][] cube = {{{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},}, {{0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0}, {0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0}}, {{0, 0, 2, 0, 0, 0, 2, 0, 0, 2, 2, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 2, 2, 2, 0, 0, 0, 2, 0, 0, 0, 0, 0}, {0, 2, 2, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0}, {2, 0, 0, 0, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0}}, {{0, 3, 0, 0, 0, 3, 0, 0, 0, 3, 3, 0, 0, 0, 0, 0}, {0, 0, 3, 0, 3, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 3, 3, 0, 0, 0, 3, 0, 0, 0, 3, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 3, 3, 3, 0, 3, 0, 0, 0, 0, 0, 0, 0}}, {{0, 0, 0, 0, 0, 4, 4, 0, 0, 4, 4, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 4, 4, 0, 0, 4, 4, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 4, 4, 0, 0, 4, 4, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 4, 4, 0, 0, 4, 4, 0, 0, 0, 0, 0}}, {{0, 5, 0, 0, 0, 5, 5, 0, 0, 0, 5, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 5, 5, 0, 5, 5, 0, 0, 0, 0, 0, 0}, {0, 5, 0, 0, 0, 5, 5, 0, 0, 0, 5, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 5, 5, 0, 5, 5, 0, 0, 0, 0, 0, 0}}, {{0, 6, 0, 0, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 6, 0, 0, 6, 6, 0, 0, 0, 6, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 6, 6, 6, 0, 0, 6, 0, 0, 0, 0, 0, 0}, {0, 6, 0, 0, 0, 6, 6, 0, 0, 6, 0, 0, 0, 0, 0, 0}}, {{0, 0, 7, 0, 0, 7, 7, 0, 0, 7, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 7, 7, 0, 0, 0, 7, 7, 0, 0, 0, 0, 0}, {0, 0, 7, 0, 0, 7, 7, 0, 0, 7, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 7, 7, 0, 0, 0, 7, 7, 0, 0, 0, 0, 0}},};

    class tetris
    {
        public int Type;
        public int X;
        public int Y;
        public int Status;
    }

    // imageView 的宽高比是在 xSize 取 10 时算出的
    // xSize 取其他值时也能正常显示 不过 imageView 会出现空白部分
    final int xSize = 10;
    final int ySize = 2 * xSize;
    final int buffer = -4;      // 下落起点 Y 坐标
    final int hardModeThreshold = 20;   // 若开启困难模式 则每下落多少个方块产生一行障碍

    // 这俩数值会在布局重置时自动算出
    float multiple = 1;
    float cubeSize = 1;

    float offsetX = 0;
    float offsetY = 0;

    byte[][] arr = new byte[xSize][ySize];

    boolean gameOver = true;
    boolean run = false;       // 或者用 timer1.Enabled
    boolean reseted = false;
    boolean cheatMode = false;

    int lineSum;
    int fixedSum;

    boolean Preview = true;
    boolean HardMode = false;

    tetris now = new tetris();
    tetris next = new tetris();

    enum action
    {
        UP,
        DOWN,
        LEFT,
        RIGHT,
    }

    // 获取方块的下落位置
    int getPreview()
    {
        for (int i = now.Y + 1; i < ySize; i++)
        {
            if (handlePosition(now.Type, now.X, i, now.Status) != 0)
            {
                return i - 1;
            }
        }
        return now.Y;
    }

    // 判断方块是否能继续向下移动
    void moveDown()
    {
        if (handlePosition(now.Type, now.X, now.Y + 1, now.Status) == 0)
        {
            now.Y += 1;
        }
        else
        {
            downSet(false);
        }
    }

    // 默认使用 getPreview() 获取下落终点 若参数为 false 则认为 now.Y 下落终点
    void downSet()
    {
        downSet(true);
    }

    void downSet(boolean preview)
    {
        int tempY = (preview) ? getPreview() : now.Y;
        handlePosition(now.Type, now.X, tempY, now.Status, true);
        fixedSum += 1;
        checkRemove();
        randomNew();

        if (HardMode && fixedSum % hardModeThreshold == 0)
        {
            insertRandomLine();
        }

        if (cheatMode)
        {
            int t = 0;
            if ((t = (fixedSum - 10) / 5) > lineSum)
            {
                lineSum = t;
            }
            while (checkExist(3))
            {
                removeLine(ySize - 1);
            }
        }
        else
        {
            // 最上层有元素时游戏结束
            if (checkExist(0))
            {
                setGameState.sendEmptyMessage(stateGAMEOVER);
            }
        }
    }

    // 在最下面一行随机追加一行做障碍
    void insertRandomLine()
    {
        removeLine(0, false);

        // 随机生成的一行既不能为空 也不能全满
        int bin = random.nextInt((1 << xSize) - 1) + 1;
        // 按位取出每个元素
        for (int i = 0; i < xSize; i++)
        {
            arr[i][ySize - 1] = (bin & (1 << i)) != 0 ? (byte) 8 : (byte) 0;
        }
    }

    // 判断第 y 行是否存在元素
    boolean checkExist(int y)
    {
        for (int i = 0; i < xSize; i++)
        {
            if (arr[i][y] != 0)
            {
                return true;
            }
        }
        return false;
    }

    // 使方块水平移动 参数为真向左移 反之向右
    void moveH(boolean left)
    {
        if (handlePosition(now.Type, now.X + (left ? -1 : 1), now.Y, now.Status) == 0)
        {
            now.X += (left ? -1 : 1);
        }
    }

    // 方块旋转
    void rotate()
    {
        int sta = now.Status + 1;
        if (sta >= 4)
            sta = 0;

        int t = handlePosition(now.Type, now.X, now.Y, sta);

        if (t == 0)
        {
            now.Status = sta;
        }
        else
        {
            for (int i = 1; i < 3; i++)         // 旋转失败 向左右移位并重新检查
            {
                if (handlePosition(now.Type, now.X + i, now.Y, sta) == 0)
                {
                    now.Status = sta;
                    now.X += i;
                    return;
                }
                else if (handlePosition(now.Type, now.X - i, now.Y, sta) == 0)
                {
                    now.Status = sta;
                    now.X -= i;
                    return;
                }
            }
        }
    }

    // 移除所有可以消除的行
    void checkRemove()
    {
        for (int i = ySize - 1; i >= 0; i--)
        {
            if (checkLine(i))
            {
                removeLine(i);
                lineSum += 1;
                i++;  // 由于该行被上一行覆盖消除 所以重新检查
            }
        }
    }

    // 检查参数指向的行是否可消除
    boolean checkLine(int y)
    {
        for (int i = 0; i < xSize; i++)
        {
            if (arr[i][y] == 0)
            {
                return false;
            }
        }
        return true;
    }

    // 移除参数指向的行 参数为真用上面的行覆盖 反之用下面的行覆盖 (可利用上移在尾行产生障碍物)
    void removeLine(int y)
    {
        removeLine(y, true);
    }

    void removeLine(int y, boolean up)
    {
        for (int i = 0; i < xSize; i++)
        {
            if (up)
            {
                for (int j = y - 1; j >= 0; j--)
                {
                    arr[i][j + 1] = arr[i][j];
                    arr[i][j] = 0;
                }
            }
            else
            {
                for (int j = y + 1; j < ySize; j++)
                {
                    arr[i][j - 1] = arr[i][j];
                    arr[i][j] = 0;
                }
            }
        }
    }

    // 方块位置处理主函数
    int handlePosition(int type, int x, int y, int status)
    {
        return handlePosition(type, x, y, status, false);
    }

    int handlePosition(int type, int x, int y, int status, boolean update)
    {
        for (int i = 0; i < 4; i++)
        {
            for (int j = 0; j < 4; j++)
            {
                if (cube[type][status][j * 4 + i] != 0)
                {
                    if (i + x < 0)      // 出左界
                    {
                        return 1;
                    }
                    else if (i + x >= xSize)    // 出右界
                    {
                        return 2;
                    }
                    else if (j + y >= ySize)    // 出底界
                    {
                        return 3;
                    }
                    else if (j + y < buffer)    // 超出缓冲区
                    {
                        return 4;
                    }
                    else if (j + y < 0)         // 在缓冲区中
                    {
                        continue;
                    }
                    else if (arr[i + x][j + y] != 0)        // 与现有方块冲突
                    {
                        return 5;
                    }
                    else if (update)        // 固定该方块
                    {
                        arr[i + x][j + y] = cube[type][status][j * 4 + i];
                    }
                }
            }
        }
        return 0;
    }

    // 随机生成一个新的方块
    void randomNew()
    {
        now.X = xSize / 2 - 2;
        now.Y = buffer;
        now.Type = next.Type;
        now.Status = next.Status;

        next.Type = random.nextInt(7) + 1;
        next.Status = (byte) random.nextInt(4);
    }

    // 重置 / 初始化
    void reset()
    {
        lineSum = 0;
        fixedSum = 0;

        for (int i = 0; i < xSize; i++)
        {
            for (int j = 0; j < ySize; j++)
            {
                arr[i][j] = 0;
            }
        }

        next.Type = random.nextInt(7) + 1;
        next.Status = (byte) random.nextInt(4);

        randomNew();
    }

    void drawTetris()
    {
        textView1.setText(getString(R.string.fixed_cube_sum) + fixedSum);
        textView2.setText(getString(R.string.remove_line_sum) + lineSum);
        textView3.setText(getString(R.string.score) + (fixedSum * 2 + lineSum * 10));

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);

        Paint pen = new Paint();
        pen.setAntiAlias(true);
        pen.setStyle(Paint.Style.STROKE);

        canvas.drawColor(Color.alpha(Color.WHITE));
        canvas.drawRect(new RectF(offsetX, offsetY, offsetX + multiple * (xSize + 0.5f), offsetY + multiple * (ySize + 0.5f)), paint);
        canvas.drawRect(new RectF(offsetX + multiple * (xSize + 1.25f), offsetY, offsetX + multiple * (xSize + 5.75f), offsetY + multiple * 4.5f), paint);

        int t = 0;

        for (int i = 0; i < 4; i++)
        {
            for (int j = 0; j < 4; j++)
            {
                if (cube[next.Type][next.Status][j * 4 + i] != 0)
                {
                    paint.setColor(color[next.Type]);
                    canvas.drawCircle(offsetX + i * multiple + multiple * (xSize + 2f), offsetY + j * multiple + multiple * 0.75f, cubeSize / 2, paint);
                }
            }
        }

        for (int j = 0; j < ySize; j++)
        {
            for (int i = 0; i < xSize; i++)
            {
                if (arr[i][j] != 0)
                {
                    paint.setColor(color[arr[i][j]]);
                    canvas.drawCircle(offsetX + i * multiple + multiple * 0.75f, offsetY + j * multiple + multiple * 0.75f, cubeSize / 2, paint);
                }

            }
        }

        int preview = 0;
        if (Preview)
        {
            preview = getPreview();
        }

        for (int i = 0; i < 4; i++)
        {
            for (int j = 0; j < 4; j++)
            {
                t = cube[now.Type][now.Status][j * 4 + i];
                if (t != 0)
                {
                    if (Preview && j + preview >= 0)
                    {
                        pen.setColor(color[t]);
                        canvas.drawCircle(offsetX + (i + now.X) * multiple + multiple * 0.75f, offsetY + (j + preview) * multiple + multiple * 0.75f, cubeSize / 2, pen);
                    }
                    if (j + now.Y >= 0)
                    {
                        paint.setColor(color[t]);
                        canvas.drawCircle(offsetX + (i + now.X) * multiple + multiple * 0.75f, offsetY + (j + now.Y) * multiple + multiple * 0.75f, cubeSize / 2, paint);
                    }
                }
            }
        }
        imageView.setImageBitmap(bitmap);
    }

    void moveTetris(action act)
    {
        if (!run || gameOver)
            return;
        switch (act)
        {
            case LEFT:
                moveH(true);
                break;
            case RIGHT:
                moveH(false);
                break;
            case UP:
                rotate();
                break;
            case DOWN:
                downSet();
                break;
        }

        drawTetris();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        random = new Random();
        imageView = (ImageView) findViewById(R.id.imageView);

        textView = (TextView) findViewById(R.id.textView);
        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView3 = (TextView) findViewById(R.id.textView3);

        timer = new Timer();
        timer.schedule(timerTask, 0, 400);

        imageView.setOnTouchListener(onTouchListener);
    }

    View.OnTouchListener onTouchListener = new View.OnTouchListener()
    {
        private float lastX = 0, lastY = 0;

        public boolean onTouch(View view, MotionEvent event)
        {
            if (event.getPointerCount() > 1)
            {
                if (!cheatMode && event.getPointerCount() == 5)
                {
                    cheatMode = true;
                    Toast.makeText(MainActivity.this, R.string.cheat_enable, Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            float x = event.getRawX();
            float y = event.getRawY();
            float minusX = x - lastX;
            float minusY = y - lastY;

            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                {

                    lastX = x;
                    lastY = y;
                    break;
                }
                // 方块旋转和下落只在抬起时触发 且触发距离是左右移动的 2 倍 防止误触或一次触发多个动作
                case MotionEvent.ACTION_UP:
                {
                    if (Math.abs(minusY) > Math.abs(minusX))
                    {
                        if (Math.abs(minusY) > 2 * multiple)
                        {
                            moveTetris(minusY > 0 ? action.DOWN : action.UP);
                            lastX = x;
                            lastY = y;
                            break;
                        }
                    }
                }

                case MotionEvent.ACTION_MOVE:
                {
                    if (Math.abs(minusX) > Math.abs(minusY))
                    {
                        if (Math.abs(minusX) > multiple)
                        {
                            moveTetris(minusX > 0 ? action.RIGHT : action.LEFT);
                            lastX = x;
                            lastY = y;
                            break;
                        }
                    }
                    break;
                }
            }
            return true;
        }
    };

    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        if (hasFocus)
        {
            if (bitmap == null)
            {
                bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
                canvas = new Canvas(bitmap);
                multiple = imageView.getWidth() / (xSize + 5.75f);
                cubeSize = imageView.getWidth() * (xSize - 0.5f) / (xSize * (xSize + 5.75f));
                offsetY = imageView.getHeight() - multiple * (ySize + 0.5f);
                setGameState.sendEmptyMessage(stateGAMERESET);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        switch (id)
        {
            case R.id.pause_or_go:
            {
                if (run)
                {
                    setGameState.sendEmptyMessage(stateGAMEPAUSE);
                }
                else
                {
                    setGameState.sendEmptyMessage(stateGAMERUN);
                }
                break;
            }
            case R.id.preview:
            {
                Preview = !Preview;
                drawTetris();
                break;
            }
            case R.id.hard_mode:
            {
                HardMode = !HardMode;
                break;
            }
            case R.id.cheat_mode:
            {
                Toast.makeText(getApplicationContext(), R.string.cheat_disable, Toast.LENGTH_SHORT).show();
                cheatMode = false;
                break;
            }
            case R.id.reset:
            {
                setGameState.sendEmptyMessage(stateGAMERESET);
                break;
            }
            case R.id.about:
            {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.dialog_text)
                        .setPositiveButton(R.string.dialog_ok, null)
                        .show();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN)
        {
            if ((System.currentTimeMillis() - exitTime) > 2000)
            {
                Toast.makeText(getApplicationContext(), R.string.press_again_to_exit, Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            }
            else
            {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu)
    {
        if (menu == null)
        {
            return true;
        }

        MenuItem pauseItem;
        MenuItem resetItem;
        MenuItem cheatItem;

        pauseItem = menu.findItem(R.id.pause_or_go);
        pauseItem.setVisible(!gameOver);
        resetItem = menu.findItem(R.id.reset);
        resetItem.setVisible(!reseted);
        cheatItem = menu.findItem(R.id.cheat_mode);
        cheatItem.setVisible(cheatMode);
        cheatItem.setChecked(cheatMode);

        menu.findItem(R.id.preview).setChecked(Preview);
        menu.findItem(R.id.hard_mode).setChecked(HardMode);

        if (!gameOver)
        {
            if (run)
            {
                pauseItem.setTitle(R.string.menu_click_to_pause);
            }
            else
            {
                pauseItem.setTitle(R.string.menu_click_to_start);
            }
        }

        return super.onMenuOpened(featureId, menu);
    }
}
