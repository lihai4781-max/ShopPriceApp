package com.homestore.price;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

public class EditItemActivity extends AppCompatActivity {

    private static final int REQ_TAKE = 101;
    private static final int REQ_PICK = 102;

    private Item item;
    private byte[] pendingPhoto;
    private boolean removePhoto = false;
    private ImageView ivPhoto;
    private final boolean[] boxPriceTouched = {false};
    private final boolean[] linking = {false};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String id = getIntent().getStringExtra("item_id");
        Item target = null;
        if (id != null) {
            for (Item it : ItemStore.load(this)) {
                if (id.equals(it.id)) {
                    target = it;
                    break;
                }
            }
        }

        item = target != null ? target : new Item();
        if (item.id == null) {
            item.id = java.util.UUID.randomUUID().toString();
            item.createdAt = System.currentTimeMillis();
        }
        if (item.tagId == null) {
            String fromTag = getIntent().getStringExtra("tag_id");
            if (fromTag != null && !fromTag.isEmpty()) {
                item.tagId = fromTag;
            }
        }
        final boolean isNew = target == null;
        final boolean isFc = getIntent().getBooleanExtra("fc", false);
        setTitle(isNew ? (isFc ? "添加鞭炮" : "添加商品") : (isFc ? "修改鞭炮" : "修改商品"));
        if (isFc) {
            item.tagId = null;
        }

        ivPhoto = findViewById(R.id.ivPhoto);
        final EditText etName = findViewById(R.id.etName);
        final EditText etCost = findViewById(R.id.etCost);
        final EditText etPrice = findViewById(R.id.etPrice);
        final EditText etBoxQty = findViewById(R.id.etBoxQty);
        final EditText etBoxCost = findViewById(R.id.etBoxCost);
        final EditText etBoxPrice = findViewById(R.id.etBoxPrice);
        final Button btnDelete = findViewById(R.id.btnDelete);

        float fs = AppPrefs.getFontScale(this);
        etName.setTextSize(16 * fs);
        etCost.setTextSize(16 * fs);
        etPrice.setTextSize(16 * fs);
        etBoxQty.setTextSize(16 * fs);
        etBoxCost.setTextSize(16 * fs);
        etBoxPrice.setTextSize(16 * fs);
        ((Button) findViewById(R.id.btnSave)).setTextSize(16 * fs);
        btnDelete.setTextSize(15 * fs);
        AppPrefs.scaleLabels(findViewById(android.R.id.content), fs);

        etName.setText(item.name);
        etPrice.setText(item.price == 0 ? "" : fmtNum(item.price));
        if (item.boxQty > 0) {
            etBoxQty.setText(String.valueOf(item.boxQty));
        }
        if (item.boxPrice > 0) {
            etBoxPrice.setText(fmtNum(item.boxPrice));
            boxPriceTouched[0] = true;
        }
        showPhoto(item.photo);

        final TextView tvShop = findViewById(R.id.tvShop);
        View layoutShop = findViewById(R.id.layoutShop);
        if (isFc) {
            layoutShop.setVisibility(View.GONE);
        } else {
            updateShopText(tvShop);
            tvShop.setOnClickListener(v -> {
            List<Tag> tags = ItemStore.loadTags(this);
            String[] names = new String[tags.size() + 1];
            names[0] = "不选进货老板";
            int current = 0;
            for (int i = 0; i < tags.size(); i++) {
                names[i + 1] = tags.get(i).name;
                if (item.tagId != null && item.tagId.equals(tags.get(i).id)) {
                    current = i + 1;
                }
            }
            ChoiceDialog.show(this, "选择进货老板", names, current, w -> {
                if (w == 0) {
                    item.tagId = null;
                } else {
                    item.tagId = tags.get(w - 1).id;
                }
                updateShopText(tvShop);
            });
        });
        }

        final boolean[] costUnlocked = {isNew};
        if (isNew) {
            etCost.setText("");
            etBoxCost.setText(item.boxCost == 0 ? "" : fmtNum(item.boxCost));
        } else {
            etCost.setText("••••");
            etCost.setFocusable(false);
            etBoxCost.setText("••••");
            etBoxCost.setFocusable(false);
            View.OnClickListener unlock = v -> {
                if (costUnlocked[0]) {
                    return;
                }
                PassDialog.show(this, "查看/修改成本价（密码）", () -> {
                    costUnlocked[0] = true;
                    etCost.setText(item.cost == 0 ? "" : fmtNum(item.cost));
                    etBoxCost.setText(item.boxCost == 0 ? "" : fmtNum(item.boxCost));
                    etCost.setFocusableInTouchMode(true);
                    etCost.setFocusable(true);
                    etBoxCost.setFocusableInTouchMode(true);
                    etBoxCost.setFocusable(true);
                });
            };
            etCost.setOnClickListener(unlock);
            etBoxCost.setOnClickListener(unlock);
        }

        android.text.TextWatcher linkWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                relinkBoxes(etBoxQty, etBoxCost, etCost, etPrice, etBoxPrice, costUnlocked[0]);
            }
        };
        etBoxQty.addTextChangedListener(linkWatcher);
        etBoxCost.addTextChangedListener(linkWatcher);
        etPrice.addTextChangedListener(linkWatcher);
        etBoxPrice.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (!linking[0]) {
                    boxPriceTouched[0] = true;
                }
            }
        });

        ivPhoto.setOnClickListener(v -> {
            CharSequence[] opts = {"拍照", "从相册选择", "删除照片"};
            ChoiceDialog.showMenu(this, "商品图片", opts, (d, w) -> {
                if (w == 0) {
                    takePhoto(item);
                } else if (w == 1) {
                    pickPhoto();
                } else {
                    pendingPhoto = null;
                    removePhoto = true;
                    ivPhoto.setImageResource(R.drawable.ic_photo_placeholder);
                    Toast.makeText(this, "将在保存后生效", Toast.LENGTH_SHORT).show();
                }
            });
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "请输入商品名称", Toast.LENGTH_SHORT).show();
                return;
            }
            double newPrice = parse(etPrice);
            if (newPrice <= 0) {
                Toast.makeText(this, "单个售价必填，请输入卖给顾客的价格", Toast.LENGTH_SHORT).show();
                return;
            }
            item.name = name;
            double oldPrice = item.price;
            if (costUnlocked[0]) {
                item.cost = parse(etCost);
                item.boxCost = parse(etBoxCost);
            }
            item.price = newPrice;
            item.boxQty = (int) parse(etBoxQty);
            item.boxPrice = parse(etBoxPrice);
            if (item.boxQty == 0) {
                item.boxCost = 0;
                item.boxPrice = 0;
            }
            if (oldPrice != item.price) {
                item.addHistory(new java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
                        .format(new java.util.Date()) + " 售价 " + fmtNum(oldPrice) + " → " + fmtNum(item.price));
            }
            item.updatedAt = System.currentTimeMillis();
            item.fc = isFc;
            if (isFc) {
                item.tagId = null;
            }
            if (pendingPhoto != null) {
                savePhotoBytes(item.id, pendingPhoto);
                item.photo = item.id + ".jpg";
            } else if (removePhoto && item.photo != null) {
                ItemStore.deletePhoto(this, item.photo);
                item.photo = null;
            }
            List<Item> all = ItemStore.load(this);
            all.removeIf(x -> x.id != null && x.id.equals(item.id));
            all.add(item);
            ItemStore.save(this, all);
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
            finish();
        });

        if (isNew) {
            btnDelete.setVisibility(View.GONE);
        } else {
            btnDelete.setOnClickListener(v -> PassDialog.show(this, "删除商品需要密码", () ->
                    ChoiceDialog.confirm(this, "删除商品", "确定删除「" + item.name + "」吗？", "删除", () -> {
                        List<Item> all = ItemStore.load(this);
                        for (Item it : all) {
                            if (it.id != null && it.id.equals(item.id)) {
                                ItemStore.deletePhoto(this, it.photo);
                            }
                        }
                        all.removeIf(x -> x.id != null && x.id.equals(item.id));
                        ItemStore.save(this, all);
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                        finish();
                    })));
        }
    }

    private void relinkBoxes(EditText etBoxQty, EditText etBoxCost, EditText etCost,
                             EditText etPrice, EditText etBoxPrice, boolean costUnlocked) {
        if (linking[0]) {
            return;
        }
        linking[0] = true;
        try {
            int qty = (int) parse(etBoxQty);
            double boxCost = parse(etBoxCost);
            double price = parse(etPrice);
            if (qty > 0) {
                if (costUnlocked && boxCost > 0) {
                    etCost.setText(fmtNum(round2(boxCost / qty)));
                }
                if (!boxPriceTouched[0] && price > 0) {
                    etBoxPrice.setText(fmtNum(price * qty));
                }
            }
        } finally {
            linking[0] = false;
        }
    }

    private void takePhoto(Item target) {
        try {
            File tmp = new File(ItemStore.photosDir(this), "tmp_" + item.id + ".jpg");
            tmp.delete();
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", tmp);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(i, REQ_TAKE);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开相机：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void pickPhoto() {
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(Intent.createChooser(i, "选择商品图片"), REQ_PICK);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开相册", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        byte[] raw = null;
        if (requestCode == REQ_TAKE) {
            File tmp = new File(ItemStore.photosDir(this), "tmp_" + item.id + ".jpg");
            if (tmp.exists()) {
                raw = ItemStore.compressImage(readAll(tmp));
                tmp.delete();
            }
        } else if (requestCode == REQ_PICK && data != null && data.getData() != null) {
            try (InputStream is = getContentResolver().openInputStream(data.getData())) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) {
                    bos.write(buf, 0, n);
                }
                raw = ItemStore.compressImage(bos.toByteArray());
            } catch (Exception e) {
                Toast.makeText(this, "读取图片失败", Toast.LENGTH_SHORT).show();
            }
        }
        if (raw != null) {
            pendingPhoto = raw;
            removePhoto = false;
            Bitmap bm = BitmapFactory.decodeByteArray(raw, 0, raw.length);
            ivPhoto.setImageBitmap(bm);
        }
    }

    private byte[] readAll(File f) {
        try (InputStream is = new java.io.FileInputStream(f)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private void showPhoto(String photo) {
        Bitmap bm = ItemStore.decodeThumb(this, photo, 600);
        if (bm != null) {
            ivPhoto.setImageBitmap(bm);
        } else {
            ivPhoto.setImageResource(R.drawable.ic_photo_placeholder);
        }
    }

    private void updateShopText(TextView tvShop) {
        String text = "不选进货老板（点击选择）";
        if (item.tagId != null) {
            for (Tag t : ItemStore.loadTags(this)) {
                if (item.tagId.equals(t.id)) {
                    text = t.name;
                    break;
                }
            }
        }
        tvShop.setText(text);
    }

    private void savePhotoBytes(String id, byte[] bytes) {
        try {
            File out = new File(ItemStore.photosDir(this), id + ".jpg");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                fos.write(bytes);
            }
        } catch (Exception e) {
            Toast.makeText(this, "保存图片失败", Toast.LENGTH_SHORT).show();
        }
    }

    private static double parse(EditText et) {
        try {
            return Double.parseDouble(et.getText().toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static double round2(double d) {
        return Math.round(d * 100.0) / 100.0;
    }

    private static String fmtNum(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
