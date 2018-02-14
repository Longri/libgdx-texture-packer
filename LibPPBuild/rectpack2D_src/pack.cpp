#include "pack.h"
#include <cstring>
#include <algorithm>

using namespace std;

bool area(rect_xywhf *a, rect_xywhf *b) {
    return a->area() > b->area();
}

bool perimeter(rect_xywhf *a, rect_xywhf *b) {
    return a->perimeter() > b->perimeter();
}

bool max_side(rect_xywhf *a, rect_xywhf *b) {
    return std::max(a->w, a->h) > std::max(b->w, b->h);
}

bool max_width(rect_xywhf *a, rect_xywhf *b) {
    return a->w > b->w;
}

bool max_height(rect_xywhf *a, rect_xywhf *b) {
    return a->h > b->h;
}


// just add another comparing function name to cmpf to perform another packing attempt
// more functions == slower but probably more efficient cases covered and hence less area wasted

bool (*cmpf[])(rect_xywhf *, rect_xywhf *) = {
        area,
        perimeter,
        max_side,
        max_width,
        max_height
};

// if you find the algorithm running too slow you may double this factor to increase speed but also decrease efficiency
// 1 == most efficient, slowest
// efficiency may be still satisfying at 64 or even 256 with nice speedup

int discard_step = 128;

/*

For every sorting function, algorithm will perform packing attempts beginning with a bin with width and height equal to max_side,
and decreasing its dimensions if it finds out that rectangles did actually fit, increasing otherwise.
Although, it's doing that in sort of binary search manner, so for every comparing function it will perform at most log2(max_side) packing attempts looking for the smallest possible bin size.
discard_step = 128 means that the algorithm will break of the searching loop if the rectangles fit but "it may be possible to fit them in a bin smaller by 128"  
the bigger the value, the sooner the algorithm will finish but the rectangles will be packed less tightly.
use discard_step = 1 for maximum tightness.

the algorithm was based on http://www.blackpawn.com/texts/lightmaps/default.html
the algorithm reuses the node tree so it doesn't reallocate them between searching attempts

*/

/*************************************************************************** CHAOS BEGINS HERE */

struct node {
    struct pnode {
        node *pn;
        bool fill;

        pnode() : fill(false), pn(0) {}

        void set(int l, int t, int r, int b) {
            if (!pn) pn = new node(rect_ltrb(l, t, r, b));
            else {
                (*pn).rc = rect_ltrb(l, t, r, b);
                (*pn).id = false;
            }
            fill = true;
        }
    };

    pnode c[2];
    rect_ltrb rc;
    bool id;

    node(rect_ltrb rc = rect_ltrb()) : id(false), rc(rc) {}

    void reset(const rect_wh &r) {
        id = false;
        rc = rect_ltrb(0, 0, r.w, r.h);
        delcheck();
    }

    node *insert(rect_xywhf &img, bool allowFlip) {
        if (c[0].pn && c[0].fill) {
            if (auto newn = c[0].pn->insert(img, allowFlip)) return newn;
            return c[1].pn->insert(img, allowFlip);
        }

        if (id) return 0;
        int f = img.fits(rect_xywh(rc), allowFlip);

        switch (f) {
            case 0:
                return 0;
            case 1:
                img.flipped = false;
                break;
            case 2:
                img.flipped = true;
                break;
            case 3:
                id = true;
                img.flipped = false;
                return this;
            case 4:
                id = true;
                img.flipped = true;
                return this;
        }

        int iw = (img.flipped ? img.h : img.w), ih = (img.flipped ? img.w : img.h);

        if (rc.w() - iw > rc.h() - ih) {
            c[0].set(rc.l, rc.t, rc.l + iw, rc.b);
            c[1].set(rc.l + iw, rc.t, rc.r, rc.b);
        } else {
            c[0].set(rc.l, rc.t, rc.r, rc.t + ih);
            c[1].set(rc.l, rc.t + ih, rc.r, rc.b);
        }

        return c[0].pn->insert(img, allowFlip);
    }

    void delcheck() {
        if (c[0].pn) {
            c[0].fill = false;
            c[0].pn->delcheck();
        }
        if (c[1].pn) {
            c[1].fill = false;
            c[1].pn->delcheck();
        }
    }

    ~node() {
        if (c[0].pn) delete c[0].pn;
        if (c[1].pn) delete c[1].pn;
    }
};

rect_wh _rect2D(bool allowFlipp, rect_xywhf *const *v, int n, int max_s, vector<rect_xywhf *> &succ, vector<rect_xywhf *> &unsucc) {
    node root;

    const int funcs = (sizeof(cmpf) / sizeof(bool (*)(rect_xywhf *, rect_xywhf *)));

    rect_xywhf **order[funcs];

    for (int f = 0; f < funcs; ++f) {
        order[f] = new rect_xywhf *[n];
        std::memcpy(order[f], v, sizeof(rect_xywhf *) * n);
        sort(order[f], order[f] + n, cmpf[f]);
    }

    rect_wh min_bin = rect_wh(max_s, max_s);
    int min_func = -1, best_func = 0, best_area = 0, _area = 0, step, fit, i;

    bool fail = false;

    for (int f = 0; f < funcs; ++f) {
        v = order[f];
        step = min_bin.w / 2;
        root.reset(min_bin);

        while (true) {
            if (root.rc.w() > min_bin.w) {
                if (min_func > -1) break;
                _area = 0;

                root.reset(min_bin);
                for (i = 0; i < n; ++i)
                    if (root.insert(*v[i],allowFlipp))
                        _area += v[i]->area();

                fail = true;
                break;
            }

            fit = -1;

            for (i = 0; i < n; ++i)
                if (!root.insert(*v[i],allowFlipp)) {
                    fit = 1;
                    break;
                }

            if (fit == -1 && step <= discard_step)
                break;

            root.reset(rect_wh(root.rc.w() + fit * step, root.rc.h() + fit * step));

            step /= 2;
            if (!step)
                step = 1;
        }

        if (!fail && (min_bin.area() >= root.rc.area())) {
            min_bin = rect_wh(root.rc);
            min_func = f;
        } else if (fail && (_area > best_area)) {
            best_area = _area;
            best_func = f;
        }
        fail = false;
    }

    v = order[min_func == -1 ? best_func : min_func];

    int clip_x = 0, clip_y = 0;

    root.reset(min_bin);

    for (i = 0; i < n; ++i) {
        if (auto ret = root.insert(*v[i],allowFlipp)) {
            v[i]->x = ret->rc.l;
            v[i]->y = ret->rc.t;

            if (v[i]->flipped) {
                v[i]->flipped = false;
                v[i]->flip();
            }

            clip_x = std::max(clip_x, ret->rc.r);
            clip_y = std::max(clip_y, ret->rc.b);

            succ.push_back(v[i]);
        } else {
            unsucc.push_back(v[i]);

            v[i]->flipped = false;
        }
    }

    for (int f = 0; f < funcs; ++f)
        delete[] order[f];

    return rect_wh(clip_x, clip_y);
}


bool pack(rect_xywhf *const *v, int n, int max_s, bool allowFlipp, vector<bin> &bins) {
    rect_wh _rect(max_s, max_s);

    for (int i = 0; i < n; ++i)
        if (!v[i]->fits(_rect, allowFlipp)) return false;

    vector<rect_xywhf *> vec[2], *p[2] = {vec, vec + 1};
    vec[0].resize(n);
    vec[1].clear();
    std::memcpy(&vec[0][0], v, sizeof(rect_xywhf *) * n);

    bin *b = 0;

    while (true) {
        bins.push_back(bin());
        b = &bins[bins.size() - 1];

        b->size = _rect2D(allowFlipp,&((*p[0])[0]), static_cast<int>(p[0]->size()), max_s, b->rects, *p[1]);
        p[0]->clear();

        if (!p[1]->size()) break;

        std::swap(p[0], p[1]);
    }

    return true;
}


rect_wh::rect_wh(const rect_ltrb &rr) : w(rr.w()), h(rr.h()) {}

rect_wh::rect_wh(const rect_xywh &rr) : w(rr.w), h(rr.h) {}

rect_wh::rect_wh(int w, int h) : w(w), h(h) {}

int rect_wh::fits(const rect_wh &r, bool allowFlipp) const {
    if (w == r.w && h == r.h) return 3;
    if (allowFlipp && h == r.w && w == r.h) return 4;
    if (w <= r.w && h <= r.h) return 1;
    if (allowFlipp && h <= r.w && w <= r.h) return 2;
    return 0;
}

rect_ltrb::rect_ltrb() : l(0), t(0), r(0), b(0) {}

rect_ltrb::rect_ltrb(int l, int t, int r, int b) : l(l), t(t), r(r), b(b) {}

int rect_ltrb::w() const {
    return r - l;
}

int rect_ltrb::h() const {
    return b - t;
}

int rect_ltrb::area() const {
    return w() * h();
}

int rect_ltrb::perimeter() const {
    return 2 * w() + 2 * h();
}

void rect_ltrb::w(int ww) {
    r = l + ww;
}

void rect_ltrb::h(int hh) {
    b = t + hh;
}

rect_xywh::rect_xywh() : x(0), y(0) {}

rect_xywh::rect_xywh(const rect_ltrb &rc) : x(rc.l), y(rc.t) {
    b(rc.b);
    r(rc.r);
}

rect_xywh::rect_xywh(int x, int y, int w, int h) : x(x), y(y), rect_wh(w, h) {}

rect_xywh::operator rect_ltrb() {
    rect_ltrb rr(x, y, 0, 0);
    rr.w(w);
    rr.h(h);
    return rr;
}

int rect_xywh::r() const {
    return x + w;
};

int rect_xywh::b() const {
    return y + h;
}

void rect_xywh::r(int right) {
    w = right - x;
}

void rect_xywh::b(int bottom) {
    h = bottom - y;
}

int rect_wh::area() {
    return w * h;
}

int rect_wh::perimeter() {
    return 2 * w + 2 * h;
}


rect_xywhf::rect_xywhf(const rect_ltrb &rr) : rect_xywh(rr), flipped(false) {}

rect_xywhf::rect_xywhf(int index, int x, int y, int width, int height) : rect_xywh(x, y, width, height),
                                                                         flipped(false), index(index) {}

rect_xywhf::rect_xywhf() : flipped(false) {}

void rect_xywhf::flip() {
    flipped = !flipped;
    std::swap(w, h);
}

void packRecArray(short *valueArray, int count, int maxTexSize, bool allowFlipp, bool debug) {


    rect_xywhf rects[count], *ptr_rects[count];

    int index = 0;
    for (int i = 0; i < count; ++i) {
        rects[i] = rect_xywhf(valueArray[index + 0], //index
                              valueArray[index + 1], // x
                              valueArray[index + 2], // y
                              valueArray[index + 3], // width
                              valueArray[index + 4]); // height
        ptr_rects[i] = rects + i;
        index += 7;
    }

    vector<bin> bins;

    if (pack(ptr_rects, count, maxTexSize, allowFlipp, bins)) {
        if (debug) {
            printf("bins: %d\n", bins.size());
        }

        for (int i = 0; i < bins.size(); ++i) {
            if (debug) {
                printf("\n\nbin: %dx%d, rects: %d\n", bins[i].size.w, bins[i].size.h, bins[i].rects.size());
            }

            for (int r = 0; r < bins[i].rects.size(); ++r) {
                rect_xywhf *rect = bins[i].rects[r];

                int recIndex = rect->index * 7;
                valueArray[recIndex + 1] = static_cast<short>(rect->x);
                valueArray[recIndex + 2] = static_cast<short>(rect->y);
                valueArray[recIndex + 3] = static_cast<short>(rect->w);
                valueArray[recIndex + 4] = static_cast<short>(rect->h);
                valueArray[recIndex + 5] = static_cast<short>(rect->flipped ? 1 : 0);
                valueArray[recIndex + 6] = static_cast<short>(i);

                if (debug) {
                    printf("REC index: %d x: %d, y: %d, w: %d, h: %d, was flipped: %s\n", rect->index, rect->x,
                           rect->y, rect->w, rect->h,
                           rect->flipped ? "yes" : " no");
                }
            }
        }

        if (debug) {
            printf("\n Array result:\n");


            //print ValueArray
            index = 0;
            for (int i = 0; i < count; ++i) {
                printf("REC index: %d x: %d, y: %d, w: %d, h: %d, was flipped: %s textureIndex:  %d \n" //
                        , valueArray[index + 0] // index
                        , valueArray[index + 1] // x
                        , valueArray[index + 2] // y
                        , valueArray[index + 3] // width
                        , valueArray[index + 4] // height
                        , valueArray[index + 5] > 0 ? "yes" : " no" // flipped
                        , valueArray[index + 6]); // texture index

                index += 7;
            }
        }
    } else {
        printf("failed: there's a rectangle with width/height bigger than max_size!\n");
    }
}